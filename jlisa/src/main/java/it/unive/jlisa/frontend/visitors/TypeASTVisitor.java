package it.unive.jlisa.frontend.visitors;

import it.unive.jlisa.frontend.ParsingEnvironment;
import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.frontend.exceptions.UnsupportedStatementException;
import it.unive.jlisa.frontend.visitors.scope.UnitScope;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaInterfaceType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.Unit;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class TypeASTVisitor extends ScopedVisitor<UnitScope> implements ResultHolder<Type> {
	private Type type;

	public TypeASTVisitor(
			ParsingEnvironment environment,
			UnitScope scope) {
		super(environment, scope);
	}

	@Override
	public boolean visit(
			PrimitiveType node) {
		if (node.getPrimitiveTypeCode() == PrimitiveType.VOID) {
			type = VoidType.INSTANCE;
		}
		if (node.getPrimitiveTypeCode() == PrimitiveType.INT) {
			type = JavaIntType.INSTANCE;
		}
		if (node.getPrimitiveTypeCode() == PrimitiveType.LONG) {
			type = JavaLongType.INSTANCE;
		}
		if (node.getPrimitiveTypeCode() == PrimitiveType.SHORT) {
			type = JavaShortType.INSTANCE;
		}
		if (node.getPrimitiveTypeCode() == PrimitiveType.FLOAT) {
			type = JavaFloatType.INSTANCE;
		}
		if (node.getPrimitiveTypeCode() == PrimitiveType.DOUBLE) {
			type = JavaDoubleType.INSTANCE;
		}
		if (node.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
			type = JavaBooleanType.INSTANCE;
		}
		if (node.getPrimitiveTypeCode() == PrimitiveType.CHAR) {
			type = JavaCharType.INSTANCE;
		}
		if (node.getPrimitiveTypeCode() == PrimitiveType.BYTE) {
			type = JavaByteType.INSTANCE;
		}
		return false;
	}

	@Override
	public boolean visit(
			ArrayType node) {
		TypeASTVisitor visitor = new TypeASTVisitor(getEnvironment(), getScope());
		node.getElementType().accept(visitor);
		Type _type = visitor.getType();
		if (_type == null) {
			throw new RuntimeException(new UnsupportedStatementException("array should have a type"));
		} else if (node.getDimensions() == 0) {
			throw new RuntimeException(new UnsupportedStatementException("array should have at least one dimension"));
		} else if (node.getDimensions() == 2) {
			_type = JavaArrayType.lookup(_type.isInMemoryType() ? new JavaReferenceType(_type) : _type,
					node.getDimensions());
			type = JavaArrayType.lookup(new JavaReferenceType(_type), 0);
		} else {
			_type = JavaArrayType.lookup(_type.isInMemoryType() ? new JavaReferenceType(_type) : _type,
					node.getDimensions());
			type = _type;
		}
		return false;

	}

	@Override
	public boolean visit(
			UnionType node) {
		throw new ParsingException(
				"union-type", ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Union type not supported",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			IntersectionType node) {
		throw new ParsingException(
				"intersection-type",
				ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Intersection type not supported",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			QualifiedType node) {
		throw new ParsingException(
				"qualified-type", ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Qualified type not supported",
				getSourceCodeLocation(node));
	}

	@Override
	public boolean visit(
			NameQualifiedType node) {
		throw new ParsingException(
				"qualified-type", ParsingException.Type.UNSUPPORTED_STATEMENT,
				"Name-qualified type not supported",
				getSourceCodeLocation(node));
	}

	public static Unit getUnit(
			String name,
			Program program,
			UnitScope scope) {

		Unit direct = program.getUnit(name);
		if (direct != null)
			return direct;

		if (name.contains(".")) {
			Unit resolved = resolveQualified(name, program, scope);
			if (resolved != null)
				return resolved;
			if (LibrarySpecificationProvider.isLibraryAvailable(name)) {
				LibrarySpecificationProvider.importClass(program, name);
				return program.getUnit(name);
			}
			return null;
		}

		Unit local = scope.getLocalType(name);
		if (local != null)
			return local;

		String pkg = scope.getPackage();
		if (pkg != null) {
			Unit samePkg = program.getUnit(pkg + "." + name);
			if (samePkg != null)
				return samePkg;
		}

		String explicit = scope.getExplicitImports().get(name);
		if (explicit != null) {
			Unit u = program.getUnit(explicit);
			if (u != null)
				return u;
			if (LibrarySpecificationProvider.isLibraryAvailable(explicit)) {
				LibrarySpecificationProvider.importClass(program, explicit);
				return program.getUnit(explicit);
			}
		}

		for (String onDemandPkg : scope.getOnDemandPackages()) {
			String fqn = onDemandPkg + "." + name;
			Unit u = program.getUnit(fqn);
			if (u != null)
				return u;
			if (LibrarySpecificationProvider.isLibraryAvailable(fqn)) {
				LibrarySpecificationProvider.importClass(program, fqn);
				return program.getUnit(fqn);
			}
		}

		Unit javaLang = program.getUnit("java.lang." + name);
		if (javaLang != null)
			return javaLang;

		if (LibrarySpecificationProvider.isLibraryAvailable(name)) {
			LibrarySpecificationProvider.importClass(program, name);
			return program.getUnit(name);
		}

		return null;
	}

	private static Unit resolveQualified(
			String name,
			Program program,
			UnitScope scope) {

		String outerName = name.substring(0, name.indexOf('.'));
		String rest = name.substring(name.indexOf('.') + 1);

		// Resolve outer normally (recursive but safe)
		Unit outer = getUnit(outerName, program, scope);
		if (outer == null)
			return null;

		return resolveNested(outer, rest, program);
	}

	private static Unit resolveNested(
			Unit outer,
			String nestedPath,
			Program program) {

		String current = outer.getName(); // fully qualified
		String[] parts = nestedPath.split("\\.");

		for (String part : parts) {
			current = current + "." + part;

			Unit nested = program.getUnit(current);

			if (nested == null) {
				// Lazy library load attempt
				if (LibrarySpecificationProvider.isLibraryAvailable(current)) {
					LibrarySpecificationProvider.importClass(program, current);
					nested = program.getUnit(current);
				}
			}

			if (nested == null)
				return null;
		}

		return program.getUnit(current);
	}

	public boolean visit(
			QualifiedName node) {
		// get the qualifier
		String qName = node.getFullyQualifiedName();

		Unit u = getUnit(qName, getProgram(), scope);
		if (u == null)
			throw new UnsupportedStatementException(
					qName + " does not exist in the program, location: " + getSourceCodeLocation(node));
		if (!(u instanceof ClassUnit))
			throw new UnsupportedStatementException(u.getName() + " is not a class unit.");

		JavaClassType javaClassType = JavaClassType.lookup(u.getName());
		if (javaClassType == null)
			type = Untyped.INSTANCE;
		else
			type = javaClassType;

		return false;
	}

	@Override
	public boolean visit(
			SimpleName node) {
		Unit u = getUnit(node.getFullyQualifiedName(), getProgram(), scope);

		if (u == null)
			throw new UnsupportedStatementException(
					node.getFullyQualifiedName() + " does not exist in the program (referenced at "
							+ getSourceCodeLocation(node) + ")");

		type = Untyped.INSTANCE;
		if (u instanceof ClassUnit)
			type = JavaClassType.lookup(u.getName());
		else if (u instanceof InterfaceUnit)
			type = JavaInterfaceType.lookup(u.getName());
		else
			throw new UnsupportedStatementException(
					node.getFullyQualifiedName() + " is not a class or interface unit");

		return false;
	}

	@Override
	public boolean visit(
			ParameterizedType node) {
		TypeASTVisitor visitor = new TypeASTVisitor(getEnvironment(), getScope());
		node.getType().accept(visitor);
		Type rawType = visitor.getType();

		if (rawType == null)
			throw new UnsupportedStatementException("Parameterized type has no valid raw type: " + node);

		// we only keep the raw type (e.g., List from List<String>)
		type = rawType;
		return false;
	}

	public Type getType() {
		return type;
	}

	public Type liftToArray(
			Type t,
			VariableDeclarationFragment fragment) {
		Type type = t;
		// we do not currently support k-dim arrays, with k > 2
		if (fragment.getExtraDimensions() > 2)
			throw new ParsingException("multi-dim array", ParsingException.Type.UNSUPPORTED_STATEMENT,
					"Multi-dimensional arrays are not supported are not supported.",
					getSourceCodeLocation(fragment));

		// single-dim array
		if (fragment.getExtraDimensions() == 1)
			type = new JavaReferenceType(JavaArrayType.lookup(type, 1));

		// bidim array
		else if (fragment.getExtraDimensions() == 2)
			type = new JavaReferenceType(
					JavaArrayType.lookup(new JavaReferenceType(JavaArrayType.lookup(type, 1)), 2));

		return type;
	}

	@Override
	public Type getResult() {
		return type;
	}
}
