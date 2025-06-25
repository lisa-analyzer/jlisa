package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.cfg.expression.forloops.ForEachLoop;
import it.unive.jlisa.program.cfg.expression.forloops.ForLoop;
import it.unive.jlisa.program.cfg.expression.instrumentations.GetNextForEach;
import it.unive.jlisa.program.cfg.expression.instrumentations.HasNextForEach;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.asserts.AssertionStatement;
import it.unive.jlisa.program.cfg.statement.asserts.SimpleAssert;
import it.unive.jlisa.type.JavaTypeSystem;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import it.unive.lisa.program.cfg.statement.literal.NullLiteral;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.code.NodeList;

import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;

import java.util.List;

public class StatementASTVisitor extends JavaASTVisitor {
    private CFG cfg;
    private it.unive.lisa.program.cfg.statement.Statement first;
    private it.unive.lisa.program.cfg.statement.Statement last;
    private NodeList<CFG, it.unive.lisa.program.cfg.statement.Statement, Edge> block = new NodeList<>(new SequentialEdge());

    public StatementASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, CFG cfg) {
        super(parserContext, source, compilationUnit);
        this.cfg = cfg;
    }

    public Statement getFirst() {
        return first;
    }

    public Statement getLast() {
        return last;
    }

    public NodeList<CFG, Statement, Edge> getBlock() {
        return block;
    }

    @Override
    public boolean visit(AssertStatement node) {
    	
    	org.eclipse.jdt.core.dom.Expression expr = node.getExpression();
    	org.eclipse.jdt.core.dom.Expression msg = node.getMessage();
    	
        ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        expr.accept(exprVisitor);
        Expression expression1 = exprVisitor.getExpression(); 
        
        Statement assrt = null;
        if(msg != null) {
	        ExpressionVisitor messageVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
	        msg.accept(messageVisitor);
	        Expression expression2 = messageVisitor.getExpression(); 
	        assrt = new AssertionStatement(cfg, getSourceCodeLocation(node), expression1,expression2);
	       
        } else {
        	assrt = new SimpleAssert(cfg, getSourceCodeLocation(node), expression1);
    	}
        
        block.addNode(assrt);
        if (first == null) {
            first = assrt;
        } else {
            block.addEdge(new SequentialEdge(first, assrt));
        }
        
        last = assrt;
    	
        return false;
    }

    @Override
    public boolean visit(Block node) {
        block = new NodeList<>(new SequentialEdge());

        for (Object o : node.statements()) {
            StatementASTVisitor statementASTVisitor = new StatementASTVisitor(parserContext, source, compilationUnit, cfg);
            ((org.eclipse.jdt.core.dom.Statement) o).accept(statementASTVisitor);
            if (statementASTVisitor.getBlock().getNodes().isEmpty()) {
                // A PARSING ERROR HAPPENS. IGNORE THAT.
                return false;
            }
            block.mergeWith(statementASTVisitor.getBlock());
            if (first == null) {
                first = statementASTVisitor.getFirst();
            }
            if (last != null) {
                block.addEdge(new SequentialEdge(last, statementASTVisitor.getFirst()));
            }
            last = statementASTVisitor.getLast();
        }
        return false;
    }

    @Override
    public boolean visit(BreakStatement node) {
        parserContext.addException(
                new ParsingException("break-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Break statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        if (!node.typeArguments().isEmpty()) {
            parserContext.addException(
                    new ParsingException("constructor-invocation", ParsingException.Type.UNSUPPORTED_STATEMENT,
                            "Constructor invocation statements with type arguments are not supported.",
                            getSourceCodeLocation(node))
            );
        }


        // get the type from the descriptor
        Expression thisExpression = new VariableRef(cfg, getSourceCodeLocation(node), "this");
        List<Expression> parameters = new ArrayList<>();
        parameters.add(thisExpression);

        if (!node.arguments().isEmpty()) {
            for (Object args : node.arguments()) {
                ASTNode e  = (ASTNode) args;
                ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
                e.accept(argumentsVisitor);
                Expression expr = argumentsVisitor.getExpression();
                if (expr != null) {
                    // This parsing error should be logged in ExpressionVisitor.
                    parameters.add(expr);
                }
            }
        }
        first = new UnresolvedCall(cfg, getSourceCodeLocation(node), Call.CallType.INSTANCE, null,this.cfg.getDescriptor().getName(), parameters.toArray(new Expression[0]));
        last = first;
        block.addNode(first);

        return false;
    }

    @Override
    public boolean visit(ContinueStatement node) {
        parserContext.addException(
                new ParsingException("continue-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Continue statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(DoStatement node) {
        NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

        StatementASTVisitor doBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        node.getBody().accept(doBody);
        this.first = doBody.getFirst();
        this.last = doBody.getLast();

        if (doBody.first == null || doBody.last == null) {
            // Parsing error. Skipping...
            return false;
        }

        ExpressionVisitor whileCondition = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        node.getExpression().accept(whileCondition);
        Expression expression = whileCondition.getExpression();

        if (expression == null) {
            // Parsing error. Skipping...
            return false;
        }

        block.addNode(expression);

        block.mergeWith(doBody.getBlock());
        block.addEdge(new SequentialEdge(this.last, expression));

        block.addEdge(new TrueEdge(expression, this.first));

        Statement noop = new NoOp(this.cfg, expression.getLocation());
        block.addNode(noop);
        block.addEdge(new FalseEdge(expression, noop));

        this.last = noop;
        this.block = block;
        return false;
    }

    @Override
    public boolean visit(EmptyStatement node) {
        parserContext.addException(
                new ParsingException("empty-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Empty statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(EnhancedForStatement node) {

       NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());
        
       
       ExpressionVisitor itemVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
       node.getParameter().accept(itemVisitor);
       Expression item = itemVisitor.getExpression();
       
       ExpressionVisitor collectionVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
       node.getExpression().accept(collectionVisitor);
       Expression collection = collectionVisitor.getExpression();
       
       Expression condition = new Equal(cfg, item.getLocation(), new TrueLiteral(cfg, item.getLocation()), new HasNextForEach(cfg,item.getLocation(),collection));
	   block.addNode(condition);
	   this.first = condition;
	   
       JavaAssignment assignment = new JavaAssignment(cfg, item.getLocation(), item, new GetNextForEach(cfg,item.getLocation(),collection));
       block.addNode(assignment);
       block.addEdge(new TrueEdge(condition, assignment));
       
       StatementASTVisitor loopBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
       if(node.getBody() == null)
       	return false; // parsing error
       
       node.getBody().accept(loopBody);
       Statement noBody = new NoOp(this.cfg, condition.getLocation());
       
       boolean hasBody = loopBody.first != null && loopBody.last != null;
       if(hasBody)
    	   block.mergeWith(loopBody.getBlock());
       else
    	   block.addNode(noBody);
       
       block.addEdge( new SequentialEdge(assignment, hasBody ? loopBody.first : noBody));
       
       block.addEdge(new SequentialEdge(hasBody ? loopBody.last : noBody, condition));
       
       Statement noop = new NoOp(this.cfg, new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+1)); // added col +1 to avoid conflict with the other noop
       block.addNode(noop);
       
       block.addEdge(new FalseEdge(condition, noop));
  
       this.last = noop;
       this.block = block;
        
       ForEachLoop forEachLoop = new ForEachLoop(block, item, condition, collection, noop, loopBody.getBlock().getNodes());
        
       this.cfg.addControlFlowStructure(forEachLoop);

       return false;
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        block = new NodeList<>(new SequentialEdge());
        ExpressionVisitor expressionVisitor = new ExpressionVisitor(parserContext, this.source, this.compilationUnit, this.cfg);
        node.getExpression().accept(expressionVisitor);

        first = expressionVisitor.getExpression();
        if (first == null) {
            // PARSING ERROR. IGNORE
            return false;
        }
        first = expressionVisitor.getExpression();
        last = first;
        block.addNode(first);
        return false;
    }

    @Override
    public boolean visit(ForStatement node) {

        NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());
        
        Triple<Statement, NodeList<CFG, Statement, Edge>, Statement> initializers = visitSequentialExpressions(node.initializers());
        
        boolean hasInitalizers = initializers.getLeft() != null && initializers.getRight() != null;
        NoOp noInit = new NoOp(cfg, getSourceCodeLocation(node));
        if(hasInitalizers) {
        	block.mergeWith(initializers.getMiddle());
        	this.first = initializers.getLeft();
        } else {
        	block.addNode(noInit);
        	this.first = noInit;
        }
        
        ExpressionVisitor conditionExpr = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        if(node.getExpression() != null)
        	node.getExpression().accept(conditionExpr);
        Expression condition = conditionExpr.getExpression();
        Statement alwaysTrue = new Equal(cfg, getSourceCodeLocation(node), new TrueLiteral(cfg, getSourceCodeLocation(node)), new TrueLiteral(cfg, getSourceCodeLocation(node)));
        
        boolean hasCondition = condition != null;
        
        if(hasCondition) {
        	block.addNode(condition);
        	block.addEdge(new SequentialEdge(hasInitalizers? initializers.getRight() : noInit, condition));
        } else {
        	block.addNode(alwaysTrue);
        	block.addEdge(new SequentialEdge(hasInitalizers? initializers.getRight() : noInit, alwaysTrue));
        }

        StatementASTVisitor loopBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        if(node.getBody() == null)
        	return false; // parsing error
        
        node.getBody().accept(loopBody);
        

        Statement noBody = new NoOp(this.cfg, hasCondition ? condition.getLocation(): new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+1)); // added col +1 to avoid conflict with the other noop
        
        boolean hasBody = loopBody.first != null && loopBody.last != null;
        if(hasBody)
        	block.mergeWith(loopBody.getBlock());
        else
        	block.addNode(noBody);
        
        if(hasCondition)
        	block.addEdge( new TrueEdge(condition, hasBody ? loopBody.first : noBody));
        else
        	block.addEdge( new TrueEdge(alwaysTrue, hasBody ? loopBody.first : noBody));
        
        Triple<Statement, NodeList<CFG, Statement, Edge>, Statement> updaters = visitSequentialExpressions(node.updaters());
        block.mergeWith(updaters.getMiddle());
        
        boolean hasUpdaters= updaters.getLeft() != null && updaters.getRight() != null;
        
        Statement noop = new NoOp(this.cfg, hasCondition ? condition.getLocation(): new SourceCodeLocation(getSourceCodeLocation(node).getSourceFile(), getSourceCodeLocation(node).getLine(), getSourceCodeLocation(node).getCol()+2)); // added col +2 to avoid conflict with the other noop
        block.addNode(noop);
                
        block.addEdge(new SequentialEdge(hasBody ? loopBody.last : noBody, hasUpdaters ? updaters.getLeft() : hasCondition ? condition : alwaysTrue));
        
        if(hasCondition)
        	block.addEdge(new SequentialEdge(hasUpdaters ? updaters.getRight() : hasBody ? loopBody.last : noBody, condition));
        else
        	block.addEdge(new SequentialEdge(hasUpdaters ? updaters.getRight() : hasBody ? loopBody.last : noBody, alwaysTrue));
        
        if(hasCondition)
        	block.addEdge(new FalseEdge(condition, noop));  
        else
        	block.addEdge(new FalseEdge(alwaysTrue, noop));  
       
        this.last = noop;
        this.block = block;
        
        ForLoop forloop = new ForLoop(block, hasInitalizers ? initializers.getMiddle().getNodes() : null, hasCondition ? condition : alwaysTrue, hasUpdaters ? updaters.getMiddle().getNodes() : null, noop, loopBody.getBlock().getNodes());
        
        this.cfg.addControlFlowStructure(forloop);
 
        return false;
    }

	private Triple<Statement, NodeList<CFG, Statement, Edge>, Statement> visitSequentialExpressions(List<ASTNode> statements) {
    	block = new NodeList<>(new SequentialEdge());
    	ASTNode[] stmts = statements.toArray(new ASTNode[statements.size()]);
    	Statement prev = null;
    	Statement first = null;
		for(int i= 0; i < stmts.length; i++) {
			ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
			stmts[i].accept(visitor);
			Expression expr = visitor.getExpression();
			block.addNode(expr);
			if(i != 0)
				block.addEdge(new SequentialEdge(prev,expr));
			else {
				block.getEntries().add(expr);
				first = expr;
			}
			prev = expr;
		}
		return Triple.of(first, block, prev);
	}

	@Override
    public boolean visit(IfStatement node) {
        block = new NodeList<>(new SequentialEdge());
        ExpressionVisitor conditionVisitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        node.getExpression().accept(conditionVisitor);

        StatementASTVisitor trueVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        node.getThenStatement().accept(trueVisitor);

        StatementASTVisitor falseVisitor = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        if (node.getElseStatement() != null) {
            node.getElseStatement().accept(falseVisitor);
        }
        if (trueVisitor.getBlock().getNodes().isEmpty() || conditionVisitor.getExpression() == null || (node.getElseStatement() != null && falseVisitor.getBlock().getNodes().isEmpty())) {
            return false;
        }
        first = conditionVisitor.getExpression();

        block.addNode(conditionVisitor.getExpression());

        NodeList<CFG, Statement, Edge> trueBlock = trueVisitor.getBlock();
        block.mergeWith(trueBlock);
        block.addEdge(new TrueEdge(first, trueVisitor.getFirst()));
        if (node.getElseStatement() != null) {
            NodeList<CFG, Statement, Edge> falseBlock = falseVisitor.getBlock();
            block.mergeWith(falseBlock);
            block.addEdge(new FalseEdge(first, falseVisitor.getFirst()));
        }
        Statement noop = new NoOp(cfg, conditionVisitor.getExpression().getLocation());
        block.addNode(noop);
        block.addEdge(new SequentialEdge(trueVisitor.getLast(), noop));
        if (node.getElseStatement() != null) {
            block.addEdge(new SequentialEdge(falseVisitor.getLast(), noop));
        }
        last = noop;
        return false;
    }

    @Override
    public boolean visit(LabeledStatement node) {
        parserContext.addException(
                new ParsingException("labeled-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Labeled statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(ReturnStatement node) {
        ExpressionVisitor visitor = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        if (node.getExpression() != null) {
            node.getExpression().accept(visitor);
        }
        Expression e = visitor.getExpression();
        Statement ret;
        if (e == null) {
            ret = new Ret(cfg, getSourceCodeLocation(node));
        } else {
            ret = new Return(cfg, getSourceCodeLocation(node), e);
        }
        first = ret;
        last = ret;
        block.addNode(ret);
        /*parserContext.addException(
                new ParsingException("return-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Return statements are not supported.",
                        getSourceCodeLocation(node))
        );*/
        return false;
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        Unit unit = cfg.getDescriptor().getUnit();
        if (!(unit instanceof ClassUnit)) {
            throw new RuntimeException("The Unit must be a ClassUnit when dealing with SuperConstructorInvocation");
        }
        ClassUnit classUnit = (ClassUnit) unit;
        String superclassName = classUnit.getImmediateAncestors().iterator().next().getName();

        Expression thisExpression = new VariableRef(cfg, getSourceCodeLocation(node), "this", parserContext.getVariableStaticType(cfg, "this"));
        List<Expression> parameters = new ArrayList<>();
        parameters.add(thisExpression);

        if (!node.arguments().isEmpty()) {
            for (Object args : node.arguments()) {
                ASTNode e  = (ASTNode) args;
                ExpressionVisitor argumentsVisitor = new ExpressionVisitor(parserContext, source, compilationUnit, cfg);
                e.accept(argumentsVisitor);
                Expression expr = argumentsVisitor.getExpression();
                if (expr != null) {
                    // This parsing error should be logged in ExpressionVisitor.
                    parameters.add(expr);
                }
            }
        }

        Statement call = new UnresolvedCall(cfg, getSourceCodeLocation(node), Call.CallType.INSTANCE, null, superclassName, parameters.toArray(new Expression[0]));
        first = call;
        last = call;
        block.addNode(call);
        /*parserContext.addException(
                new ParsingException("super-constructor-invocation", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Super constructor invocations are not supported.",
                        getSourceCodeLocation(node))
        );*/
        return false;
    }

    @Override
    public boolean visit(SwitchCase node) {
        parserContext.addException(
                new ParsingException("switch-case", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Switch cases are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(SwitchStatement node) {
        parserContext.addException(
                new ParsingException("switch-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Switch statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(SynchronizedStatement node) {
        parserContext.addException(
                new ParsingException("synchronized-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Synchronized statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(ThrowStatement node) {
        parserContext.addException(
                new ParsingException("throw-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Throw statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(TryStatement node) {
        parserContext.addException(
                new ParsingException("try-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Try-catch-finally blocks are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }
    
   
	@Override
    public boolean visit(TypeDeclarationStatement node) {
        parserContext.addException(
                new ParsingException("type-declaration-statement", ParsingException.Type.UNSUPPORTED_STATEMENT,
                        "Type declaration statements are not supported.",
                        getSourceCodeLocation(node))
        );
        return false;
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        block = new NodeList<>(new SequentialEdge());
        TypeASTVisitor visitor = new TypeASTVisitor(this.parserContext, source, compilationUnit);
        node.getType().accept(visitor);
        Type variableType = visitor.getType();
        if (variableType.isInMemoryType()) {
        	variableType = new ReferenceType(variableType);
        }
        for (Object f : node.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) f;
            String variableName = fragment.getName().getIdentifier();
            SourceCodeLocation loc = getSourceCodeLocation(fragment);
            VariableRef ref = new VariableRef(cfg,
                    getSourceCodeLocation(fragment),
                    variableName, variableType);
            Expression initializer;
            parserContext.addVariableType(cfg,variableName, variableType);
            if(fragment.getInitializer() == null) {
                    initializer = JavaTypeSystem.getDefaultLiteral(variableType, cfg, loc);
            } else {
                org.eclipse.jdt.core.dom.Expression expr = fragment.getInitializer();
                ExpressionVisitor exprVisitor = new ExpressionVisitor(this.parserContext, source, compilationUnit, cfg);
                expr.accept(exprVisitor);
                initializer = exprVisitor.getExpression();
                if (initializer == null) {
                    initializer = new NullLiteral(cfg, loc);
                }
            }
            JavaAssignment assignment = new JavaAssignment(cfg, loc, ref, initializer);
            block.addNode(assignment);
            if (first == null) {
                first = assignment;
            } else {
                block.addEdge(new SequentialEdge(first, assignment));
            }
            //cfg.getNodeList().mergeWith(block);
            //cfg.addNode(ref);
            last = assignment;

            //fragment.getInitializer()
        }
        return false;
    }

    @Override
    public boolean visit(WhileStatement node) {
        NodeList<CFG, Statement, Edge> block = new NodeList<>(new SequentialEdge());

        ExpressionVisitor condition = new ExpressionVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        node.getExpression().accept(condition);
        Expression expression = condition.getExpression();

        if (expression == null) {
            // Parsing error. Skipping...
            return false;
        }

        this.first = expression;
        block.addNode(expression);

        StatementASTVisitor loopBody = new StatementASTVisitor(this.parserContext, this.source, this.compilationUnit, this.cfg);
        node.getBody().accept(loopBody);

        if (loopBody.first == null || loopBody.last == null) {
            // Parsing error. Skipping...
            return false;
        }

        block.mergeWith(loopBody.getBlock());
        block.addEdge(new TrueEdge(expression, loopBody.getFirst()));
        block.addEdge(new SequentialEdge(loopBody.getLast(), expression));

        Statement noop = new NoOp(this.cfg, expression.getLocation());
        block.addNode(noop);
        block.addEdge(new FalseEdge(expression, noop));

        this.last = noop;
        this.block = block;
        return false;
    }
}
