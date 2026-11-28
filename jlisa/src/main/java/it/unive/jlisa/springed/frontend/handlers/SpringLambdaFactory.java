package it.unive.jlisa.springed.frontend.handlers;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.handlers.LambdaFactory;
import it.unive.jlisa.frontend.util.JavaCFGTweaker;
import it.unive.jlisa.frontend.util.JavaLocalVariableTracker;
import it.unive.jlisa.frontend.util.JavaLocalVariableTracker.LocalVariable;
import it.unive.jlisa.frontend.util.VariableInfo;
import it.unive.jlisa.frontend.visitors.expression.ExpressionVisitor;
import it.unive.jlisa.frontend.visitors.scope.MethodScope;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.frontend.visitors.statement.BlockStatementASTVisitor;
import it.unive.jlisa.frontend.visitors.structure.VariableDeclarationASTVisitor;
import it.unive.jlisa.program.JavaLocation;
import it.unive.jlisa.program.SyntheticCodeLocationManager;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.cfg.JavaParameter;
import it.unive.jlisa.program.cfg.statement.JavaReturn;
import it.unive.jlisa.springed.cfg.JavaLambda;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.frontend.ControlFlowTracker;
import it.unive.lisa.util.frontend.ParsedBlock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.eclipse.jdt.core.dom.*;

public class SpringLambdaFactory implements LambdaFactory {

	@Override
	public Expression create(
			LambdaExpression node,
			ParsingEnvironment environment,
			MethodScope scope) {
		return new LambdaBuilder(node, environment, scope).build();
	}

	private class LambdaBuilder {

		private final LambdaExpression lambdaAstNode;
		private final CompilationUnit compilationUnit;

		private final ParserContext parserContext;
		private final ParsingEnvironment parsingEnvironment;
		private final String currentFileSourceName;

		private final UnitScope unitScope;
		private final MethodScope methodScope;
		private final CFG enclosingCfg;

		private final JavaLocation codeLocation;

		private LambdaBuilder(
				LambdaExpression lambdaAstNode,
				ParsingEnvironment parsingEnvironment,
				MethodScope methodScope) {
			this.lambdaAstNode = lambdaAstNode;
			this.compilationUnit = methodScope.getParentScope().getLiSACompilationUnit();
			this.parserContext = parsingEnvironment.parserContext();
			this.parsingEnvironment = parsingEnvironment;
			this.currentFileSourceName = parsingEnvironment.source();
			this.unitScope = methodScope.getParentScope().getUnitScope();
			this.methodScope = methodScope;
			this.enclosingCfg = methodScope.getCFG();
			this.codeLocation = parsingEnvironment.getSourceCodeLocation(lambdaAstNode);
		}

		private Expression build() {
			Parameter[] allParameters = this.collectEntryParams();
			JavaCodeMemberDescriptor lambdaHeader = this.buildLambdaHeader(allParameters);

			CFG lambdaCfg = this.createAndRegisterCFG(lambdaHeader);
			this.buildBody(lambdaCfg, lambdaHeader);

			Expression[] outerVariableReads = this.readOuterVariables(allParameters);
			return new JavaLambda(this.enclosingCfg, this.codeLocation, lambdaCfg, outerVariableReads);
		}

		private Parameter[] collectEntryParams() {
			List<?> declarations = this.lambdaAstNode.parameters();
			int ownParameterCount = declarations.size();

			// Firstly collecting ones that are used in the lambda, but are
			// defined outside of it
			List<String> outerLocalVariables = collectOuterLocalVariables().stream().toList();
			int outerLocalVariableCount = outerLocalVariables.size();

			Parameter[] lambdaParameters = new Parameter[ownParameterCount
					+ outerLocalVariableCount];

			for (int i = 0; i < outerLocalVariableCount; i++) {
				String name = outerLocalVariables.get(i);

				LocalVariable outerVariable = this.methodScope.getTracker().getLocalVariable(name);
				CodeLocation location = outerVariable.getLocation();

				VariableInfo outerVariableInfo = new VariableInfo(name, outerVariable);
				Type outerVariableType = this.parserContext.getVariableStaticType(this.enclosingCfg, outerVariableInfo);

				JavaParameter outerVariableAsParameter = new JavaParameter(location, name, outerVariableType);
				lambdaParameters[i] = outerVariableAsParameter;
			}

			// Secondly proceed to collecting entry parameters present in the
			// lambda itself
			for (int i = 0; i < ownParameterCount; i++) {
				var declaration = declarations.get(i);

				if (declaration instanceof SingleVariableDeclaration instance) {
					VariableDeclarationASTVisitor visitor = new VariableDeclarationASTVisitor(
							this.parsingEnvironment,
							this.unitScope);

					Parameter parameter = this.parserContext.evaluate(instance, visitor);
					lambdaParameters[outerLocalVariableCount + i] = parameter;

				} else if (declaration instanceof VariableDeclarationFragment instance) {
					JavaLocation javaLocation = this.parsingEnvironment.getSourceCodeLocation(instance);
					String name = instance.getName().getIdentifier();
					JavaParameter parameter = new JavaParameter(javaLocation, name, Untyped.INSTANCE);

					lambdaParameters[outerLocalVariableCount + i] = parameter;

				} else {
					throw new ParsingException(
							"lambda-entry-parameter",
							ParsingException.Type.MALFORMED_SOURCE,
							"Unexpected lambda parameter: " + declaration,
							this.codeLocation);
				}
			}

			return lambdaParameters;
		}

		/*
		 * Collects 'this' (in instance methods) and the enclosing method's
		 * local variables and parameters that the lambda body uses. They become
		 * extra parameters of the lambda's CFG, so that the body can resolve
		 * them.
		 */
		private Set<String> collectOuterLocalVariables() {
			JavaLocalVariableTracker enclosingMethodVariableTracker = this.methodScope.getTracker();
			Set<String> outerLocalVariables = new LinkedHashSet<>();

			if (enclosingMethodVariableTracker == null)
				return outerLocalVariables;

			// As in lambda keyword 'this' (if used) appears as variable, it is
			// then also collected
			if (enclosingMethodVariableTracker.hasVariable("this"))
				outerLocalVariables.add("this");

			ASTNode lambdaBody = this.lambdaAstNode.getBody();

			ASTVisitor nameCollector = new ASTVisitor() {
				@Override
				public boolean visit(
						SimpleName node) {

					boolean isNotMethodName = node.getLocationInParent() != MethodInvocation.NAME_PROPERTY;
					if (isNotMethodName && enclosingMethodVariableTracker.hasVariable(node.getIdentifier()))
						outerLocalVariables.add(node.getIdentifier());

					return true;
				}
			};

			lambdaBody.accept(nameCollector);

			return outerLocalVariables;
		}

		private JavaCodeMemberDescriptor buildLambdaHeader(
				Parameter[] parameters) {

			String name = "lambda$" + this.codeLocation.getLine() + "$" + this.codeLocation.getCol();

			return new JavaCodeMemberDescriptor(
					this.codeLocation,
					this.compilationUnit,
					false,
					name,
					Untyped.INSTANCE,
					parameters);
		}

		private CFG createAndRegisterCFG(
				JavaCodeMemberDescriptor lambdaHeader) {
			CFG cfg = new CFG(lambdaHeader);

			boolean isNewMember = this.compilationUnit.addCodeMember(cfg);

			if (!isNewMember) {
				throw new ParsingException(
						"duplicated_method_descriptor",
						ParsingException.Type.MALFORMED_SOURCE,
						"Duplicate descriptor " + lambdaHeader + " in unit " + this.compilationUnit.getName(),
						this.codeLocation);
			}

			return cfg;
		}

		private void buildBody(
				CFG cfg,
				JavaCodeMemberDescriptor lambdaHeader) {
			this.registerParameterTypesInContext(cfg, lambdaHeader);
			this.buildCfgBody(cfg, lambdaHeader);
		}

		/*
		 * Registers the left-side parameter types for the visitor that reads
		 * the right-side body.
		 */
		private void registerParameterTypesInContext(
				CFG cfg,
				JavaCodeMemberDescriptor lambdaHeader) {
			for (Parameter parameter : lambdaHeader.getFormals()) {
				VariableInfo variableInfo = new VariableInfo(parameter.getName(), null);
				this.parserContext.addVariableType(cfg, variableInfo, parameter.getStaticType());
			}
		}

		private void buildCfgBody(
				CFG cfg,
				JavaCodeMemberDescriptor lambdaHeader) {
			JavaLocalVariableTracker variableTracker = new JavaLocalVariableTracker(cfg, lambdaHeader);
			ControlFlowTracker controlFlowTracker = new ControlFlowTracker();

			MethodScope lambdaScope = createLambdaScope(cfg, variableTracker, controlFlowTracker);

			variableTracker.enterScope();

			var lambdaBody = this.lambdaAstNode.getBody();
			if (lambdaBody instanceof org.eclipse.jdt.core.dom.Expression rightSideExpression) {
				ExpressionVisitor visitor = new ExpressionVisitor(this.parsingEnvironment, lambdaScope);
				CodeLocation returnLocation = this.parserContext.getCurrentSyntheticCodeLocationManager(
						this.parsingEnvironment.source()).nextLocation();

				Expression resultValue = this.parserContext.evaluate(
						rightSideExpression,
						visitor);

				JavaReturn implicitReturn = new JavaReturn(cfg, returnLocation, resultValue);

				cfg.getEntrypoints().add(implicitReturn);
				cfg.addNode(implicitReturn);
				variableTracker.exitScope(implicitReturn);

			} else if (lambdaBody instanceof Block rightSideBlock) {
				BlockStatementASTVisitor visitor = new BlockStatementASTVisitor(this.parsingEnvironment, lambdaScope);

				ParsedBlock blockNodes = this.parserContext.evaluate(rightSideBlock, visitor);

				cfg.getEntrypoints().add(blockNodes.getBegin());
				cfg.getNodeList().mergeWith(blockNodes.getBody());

				finishCfg(cfg);
				variableTracker.exitScope(blockNodes.getEnd());
			}
		}

		private MethodScope createLambdaScope(
				CFG cfg,
				JavaLocalVariableTracker variableTracker,
				ControlFlowTracker controlFlowTracker) {
			return this.methodScope.getParentScope().toMethodScope(
					cfg, variableTracker, controlFlowTracker);
		}

		private void finishCfg(
				CFG cfg) {
			SyntheticCodeLocationManager currentFileSyntheticLocationManager = this.parserContext
					.getCurrentSyntheticCodeLocationManager(this.currentFileSourceName);

			Function<String, ParsingException> exceptionFactory = (
					String exceptionMessage) -> {
				return new ParsingException(
						"lambda-return",
						ParsingException.Type.MALFORMED_SOURCE,
						exceptionMessage,
						this.codeLocation);
			};

			/*
			 * Lambdas with try blocks are not expected here. To support them,
			 * call JavaCFGTweaker.splitProtectedYields and
			 * JavaCFGTweaker.addFinallyEdges before addReturns.
			 */

			JavaCFGTweaker.addReturns(cfg, exceptionFactory, currentFileSyntheticLocationManager);
			cfg.simplify();
		}

		private Expression[] readOuterVariables(
				Parameter[] allParameters) {
			int ownParameterCount = this.lambdaAstNode.parameters().size();
			int outerParameterCount = allParameters.length - ownParameterCount;
			SyntheticCodeLocationManager syntheticLocationManager = this.parserContext
					.getCurrentSyntheticCodeLocationManager(this.currentFileSourceName);

			Expression[] outerReads = new Expression[outerParameterCount];
			for (int i = 0; i < outerParameterCount; i++) {
				Parameter outerParameter = allParameters[i];

				VariableRef outerRead = new VariableRef(this.enclosingCfg, syntheticLocationManager.nextLocation(),
						outerParameter.getName(), outerParameter.getStaticType());

				outerReads[i] = outerRead;
			}

			return outerReads;
		}
	}
}