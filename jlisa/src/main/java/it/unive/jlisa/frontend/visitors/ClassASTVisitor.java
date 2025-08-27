package it.unive.jlisa.frontend.visitors;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import it.unive.jlisa.frontend.EnumUnit;
import it.unive.jlisa.frontend.ParserContext;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.cfg.statement.JavaAssignment;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessInstanceGlobal;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.lisa.program.AbstractClassUnit;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.VoidType;

public class ClassASTVisitor extends JavaASTVisitor{
	
	private boolean nested;

    public ClassASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit) {
        super(parserContext, source, compilationUnit);
        nested = false;
    }

    public ClassASTVisitor(ParserContext parserContext, String source, CompilationUnit compilationUnit, boolean nested) {
        super(parserContext, source, compilationUnit);
        this.nested = nested;
    }
    
   
	@Override
	public boolean visit(EnumDeclaration node) {
		// iterates over inner declarations
		// enum inner declaration
		EnumUnit enUnit = (EnumUnit) getProgram().getUnit(node.getName().toString());
		Type enumType = getProgram().getTypes().getType(enUnit.getName());

		// build private constructor for enum
		createEnumConstructor(enUnit, (EnumDeclaration) node);

		// adding static fields corresponding to enum constants
		for (Object con : node.enumConstants()) {
			Global g = new Global(getSourceCodeLocation((ASTNode) con), enUnit, con.toString(), false, new ReferenceType(enumType));
			enUnit.addGlobal(g);
		}
		
		return false;
	}

    
	private void computeNestedUnits(TypeDeclaration typeDecl) {

		if ((typeDecl.isInterface())) {
			InterfaceUnit iUnit = buildInterfaceUnit(source, getProgram(), typeDecl);
			getProgram().getTypes().registerType(JavaInterfaceType.lookup(iUnit.getName(), iUnit));
			getProgram().addUnit(iUnit);
		} else {
			ClassUnit cUnit = buildClassUnit(source, getProgram(), typeDecl);
			getProgram().getTypes().registerType(JavaClassType.lookup(cUnit.getName(), cUnit));
			getProgram().addUnit(cUnit);
		}
	}
    

    public InterfaceUnit buildInterfaceUnit(String source, Program program, TypeDeclaration typeDecl) {
        SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

        int modifiers = typeDecl.getModifiers();
        if (Modifier.isFinal(modifiers)) {
            throw new RuntimeException(new ProgramValidationException("Illegal combination of modifiers: interface and final"));
        }

        InterfaceUnit iUnit = new InterfaceUnit(loc, program, typeDecl.getName().toString(), false);
        program.addUnit(iUnit);
        return iUnit;
    }

    public ClassUnit buildClassUnit(String source, Program program, TypeDeclaration typeDecl) {
        SourceCodeLocation loc = getSourceCodeLocation(typeDecl);

        int modifiers = typeDecl.getModifiers();
        if (Modifier.isPrivate(modifiers) && !(typeDecl.getParent() instanceof CompilationUnit)) {
            throw new RuntimeException(new ProgramValidationException("Modifier private not allowed in a top-level class"));
        }
        ClassUnit cUnit;
        if (Modifier.isAbstract(modifiers)) {
            if (Modifier.isFinal(modifiers)) {
                throw new RuntimeException(new ProgramValidationException("illegal combination of modifiers: abstract and final"));
            }
            cUnit = new AbstractClassUnit(loc, program, typeDecl.getName().toString(), Modifier.isFinal(modifiers));
        } else {
            cUnit = new ClassUnit(loc, program, typeDecl.getName().toString(), Modifier.isFinal(modifiers));
        }
        program.addUnit(cUnit);
        return cUnit;
    }

	@Override
	public boolean visit(TypeDeclaration node) {
		
        TypeDeclaration[] types = node.getTypes(); // nested types (e.g. nested inner classes)
        
        for (Object type : types) {
            if (type instanceof TypeDeclaration) {
                TypeDeclaration typeDecl = (TypeDeclaration) type;
                if ((typeDecl.isInterface())) {
                    InterfaceASTVisitor interfaceVisitor = new InterfaceASTVisitor(parserContext, source, compilationUnit);
                    typeDecl.accept(interfaceVisitor);
                } else {
                    ClassASTVisitor classVisitor = new ClassASTVisitor(parserContext, source, compilationUnit, true);
                    typeDecl.accept(classVisitor);
                }
            }
            if (type instanceof EnumDeclaration) {
                parserContext.addException(new ParsingException("enum-declaration", ParsingException.Type.UNSUPPORTED_STATEMENT, "Enum Declarations are not supported.", getSourceCodeLocation((EnumDeclaration)type)));
            }
            if (type instanceof AnnotationTypeDeclaration) {
                parserContext.addException(new ParsingException("annotation-type-declaration", ParsingException.Type.UNSUPPORTED_STATEMENT, "Annotation Type Declarations are not supported.", getSourceCodeLocation((AnnotationTypeDeclaration)type)));
            }
        }
        
        if(nested)
        	computeNestedUnits(node);
        
		ClassUnit cUnit = (ClassUnit) getProgram().getUnit(node.getName().toString());
		if (node.getSuperclassType() != null) {
			TypeASTVisitor visitor = new TypeASTVisitor(parserContext, source, compilationUnit);
			node.getSuperclassType().accept(visitor);
			Type superType = visitor.getType();
			if (superType != null) {
				it.unive.lisa.program.Unit superUnit = getProgram().getUnit(superType.toString());
				if (superUnit instanceof it.unive.lisa.program.CompilationUnit) {
					cUnit.addAncestor((it.unive.lisa.program.CompilationUnit)superUnit);
				}
			}
		} else {
			cUnit.addAncestor(JavaClassType.lookup("Object", null).getUnit());
		}
		if (!node.permittedTypes().isEmpty()) {
			parserContext.addException(new ParsingException("permits", ParsingException.Type.UNSUPPORTED_STATEMENT, "Permits is not supported.", getSourceCodeLocation(node)));
		}

		// iterates over inner declarations
		for (Object decl : node.bodyDeclarations()) {
			// enum inner declaration
			if (decl instanceof EnumDeclaration) 
				visit((EnumDeclaration) decl);
		}

		for (FieldDeclaration fd : node.getFields()) {
			FieldDeclarationVisitor visitor = new FieldDeclarationVisitor(parserContext, source, cUnit, compilationUnit);
			fd.accept(visitor);
		}

		boolean createDefaultConstructor = true;
		for (MethodDeclaration md : node.getMethods()) {
			MethodASTVisitor visitor = new MethodASTVisitor(parserContext, source, cUnit, compilationUnit);
			md.accept(visitor);
			if (md.isConstructor()) {
				createDefaultConstructor = false;
				fixConstructorCFG(visitor.getCFG(), node.getFields());
			}
		}
		if (createDefaultConstructor) {
			CFG defaultConstructor = createDefaultConstructor(cUnit);
			fixConstructorCFG(defaultConstructor, node.getFields());
		}

		return false;
	}

	private CFG createDefaultConstructor(ClassUnit classUnit) {
		Type type = getProgram().getTypes().getType(classUnit.getName());

		List<Parameter> parameters = new ArrayList<>();
		CodeLocation unknownLocation = classUnit.getLocation();
		parameters.add(new Parameter(unknownLocation, "this", new ReferenceType(type), null, new Annotations()));

		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		CodeMemberDescriptor codeMemberDescriptor = new CodeMemberDescriptor(unknownLocation, classUnit, true, classUnit.getName(), VoidType.INSTANCE, annotations, paramArray);
		CFG cfg = new CFG(codeMemberDescriptor);
		parserContext.addVariableType(cfg, "this", new ReferenceType(type));
		String superClassName = classUnit.getImmediateAncestors().iterator().next().getName();

		Statement call = new UnresolvedCall(cfg, unknownLocation, Call.CallType.INSTANCE, null, superClassName, new VariableRef(cfg, unknownLocation, "this"));

		Ret ret = new Ret(cfg, cfg.getDescriptor().getLocation());
		cfg.addNode(ret);
		cfg.addNode(call);
		cfg.getEntrypoints().add(call);
		cfg.addEdge(new SequentialEdge(call, ret));
		classUnit.addInstanceCodeMember(cfg);
		return cfg;
	}

	private CFG createEnumConstructor(EnumUnit enumUnit, EnumDeclaration enumDecl) {
		Type type = getProgram().getTypes().getType(enumUnit.getName());

		List<Parameter> parameters = new ArrayList<>();
		CodeLocation enumDeclLoc = getSourceCodeLocation(enumDecl);
		parameters.add(new Parameter(enumDeclLoc, "this", new ReferenceType(type), null, new Annotations()));
		parameters.add(new Parameter(enumDeclLoc, "name", new ReferenceType(getProgram().getTypes().getStringType()), null, new Annotations()));

		Annotations annotations = new Annotations();
		Parameter[] paramArray = parameters.toArray(new Parameter[0]);
		CodeMemberDescriptor codeMemberDescriptor = new CodeMemberDescriptor(enumDeclLoc, enumUnit, true, enumUnit.getName(), VoidType.INSTANCE, annotations, paramArray);
		CFG cfg = new CFG(codeMemberDescriptor);
		parserContext.addVariableType(cfg, "this", new ReferenceType(type));
		parserContext.addVariableType(cfg, "name", new ReferenceType(getProgram().getTypes().getStringType()));

		JavaAssignment glAsg = new JavaAssignment(cfg, enumDeclLoc, 
				new JavaAccessInstanceGlobal(cfg, enumDeclLoc, 
						new VariableRef(cfg, enumDeclLoc, "this"), 
						"name"), 
				new VariableRef(cfg, enumDeclLoc, "name"));


		Ret ret = new Ret(cfg, cfg.getDescriptor().getLocation());
		cfg.addNode(glAsg);
		cfg.addNode(ret);
		cfg.getEntrypoints().add(glAsg);
		cfg.addEdge(new SequentialEdge(glAsg, ret));
		enumUnit.addInstanceCodeMember(cfg);
		return cfg;
	}

	public void fixConstructorCFG(CFG cfg, FieldDeclaration[] fields) {
		Statement entryPoint = cfg.getEntrypoints().iterator().next();
		Statement injectionPoint = entryPoint;
		Unit u = cfg.getDescriptor().getUnit();
		if (!(u instanceof ClassUnit)) {
			throw new RuntimeException("The unit of a constructor must be a class unit");
		}
		ClassUnit classUnit = (ClassUnit) u;
		Unit ancestor = classUnit.getImmediateAncestors().iterator().next();
		boolean implicitlyCallSuper = true;
		if (injectionPoint instanceof UnresolvedCall call) {
			if (ancestor.getName().equals(call.getConstructName())) {
				implicitlyCallSuper = false;
				List<Edge> outEdges = new ArrayList<>(cfg.getNodeList().getOutgoingEdges(injectionPoint));
				if (outEdges.size() == 1) {
					injectionPoint = outEdges.getFirst().getDestination();
				}
			}
		}

		if (injectionPoint instanceof UnresolvedCall &&
				((UnresolvedCall) injectionPoint).getConstructName().equals(cfg.getDescriptor().getName())) {
			return;
		}
		if (implicitlyCallSuper) {
			// add a super() call to this constructor, as a first statement, before the field initializator.
			CodeLocation unknownLocation = cfg.getDescriptor().getLocation();
			Statement call = new UnresolvedCall(cfg, unknownLocation, Call.CallType.INSTANCE, null, ancestor.getName(), new VariableRef(cfg, unknownLocation, "this"));
			cfg.addNode(call);
			cfg.addEdge(new SequentialEdge(call, injectionPoint));
			cfg.getEntrypoints().clear();
			cfg.getEntrypoints().add(call);
			entryPoint = call;
		}
		Statement first = null, last = null;

		for (FieldDeclaration field : fields) {
			FieldInitializationVisitor initVisitor = new FieldInitializationVisitor(parserContext, source, compilationUnit, cfg);
			field.accept(initVisitor);

			if (initVisitor.getBlock() != null) {
				cfg.getNodeList().mergeWith(initVisitor.getBlock());

				if (first == null) {
					first = initVisitor.getFirst();
				} else {
					cfg.addEdge(new SequentialEdge(last, initVisitor.getFirst()));
				}
				last = initVisitor.getLast();
			}
		}

		if (first != null) {
			if (injectionPoint.equals(entryPoint)) {
				cfg.getEntrypoints().clear();
				cfg.getEntrypoints().add(first);
				cfg.addEdge(new SequentialEdge(last, entryPoint));
			} else {
				for (Edge edge : cfg.getIngoingEdges(injectionPoint)) {
					cfg.getNodeList().removeEdge(edge);
					cfg.addEdge(new SequentialEdge(edge.getSource(), first));
				}
				cfg.addEdge(new SequentialEdge(last, injectionPoint));
			}
		}
	}
	

}