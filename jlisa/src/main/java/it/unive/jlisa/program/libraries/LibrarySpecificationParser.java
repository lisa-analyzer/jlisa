package it.unive.jlisa.program.libraries;

import it.unive.jlisa.antlr.LibraryDefinitionParser.*;
import it.unive.jlisa.antlr.LibraryDefinitionParserBaseVisitor;
import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.libraries.loader.*;
import it.unive.jlisa.program.libraries.loader.Runtime;
import java.util.Collection;
import java.util.LinkedList;
import org.apache.commons.lang3.tuple.Pair;

public class LibrarySpecificationParser extends LibraryDefinitionParserBaseVisitor<Object> {

	private final String file;
	private final SourceCodeLocationManager locationManager;

	public LibrarySpecificationParser(
			String file) {
		this.file = file;
		this.locationManager = new SourceCodeLocationManager(
				"$java_runtime" + file.substring(0, file.lastIndexOf('.')));
	}

	@Override
	public ClassDef visitClassDef(
			ClassDefContext ctx) {
		ClassDef cls = new ClassDef(
				ctx.ROOT() != null,
				ctx.SEALED() != null,
				ctx.type_name == null ? null : ctx.type_name.getText(),
				ctx.name.getText(),
				ctx.base == null ? null : ctx.base.getText());
		for (MethodContext mtd : ctx.method())
			cls.getMethods().add(visitMethod(mtd));

		for (FieldContext fld : ctx.field())
			cls.getFields().add(visitField(fld));
		return cls;
	}

	@Override
	public Field visitField(
			FieldContext ctx) {
		return new Field(
				ctx.INSTANCE() != null,
				ctx.name.getText(),
				visitType(ctx.type()));
	}

	@Override
	public Parameter visitParam(
			ParamContext ctx) {
		Type type = visitType(ctx.type());
		String name = ctx.name.getText();
		if (ctx.DEFAULT() == null)
			return new Parameter(name, type);

		Value def;
		if (ctx.val.NONE() != null)
			def = new NoneValue();
		else if (ctx.val.BOOLEAN() != null)
			def = new BooleanValue(ctx.val.BOOLEAN().getText().equals("true"));
		else if (ctx.val.STRING() != null)
			def = new StringValue(ctx.val.STRING().getText());
		else if (ctx.val.NUMBER() != null)
			def = new NumberValue(Integer.parseInt(ctx.val.NUMBER().getText()));
		else
			throw new LibraryParsingException(file, "Unsupported default parameter type: " + type);

		return new Parameter(name, type, def);
	}

	@Override
	public Type visitType(
			TypeContext ctx) {
		if (ctx.libtype() != null)
			return visitLibtype(ctx.libtype());
		else
			return visitLisatype(ctx.lisatype());
	}

	@Override
	public Type visitLibtype(
			LibtypeContext ctx) {
		return new LibType(ctx.type_name.getText(), ctx.STAR() != null);
	}

	@Override
	public Type visitLisatype(
			LisatypeContext ctx) {
		return new LiSAType(ctx.type_name.getText(), ctx.type_field.getText());
	}

	@Override
	public Method visitMethod(
			MethodContext ctx) {
		Method mtd = new Method(
				ctx.INSTANCE() != null,
				ctx.SEALED() != null,
				ctx.name.getText(),
				ctx.implementation.getText(),
				visitType(ctx.type()));
		for (int i = 0; i < ctx.param().size(); i++)
			mtd.getParams().add(visitParam(ctx.param(i)));
		return mtd;
	}

	@Override
	public Library visitLibrary(
			LibraryContext ctx) {
		Library lib = new Library(ctx.name.getText(), locationManager);
		for (MethodContext mtd : ctx.method())
			lib.getMethods().add(visitMethod(mtd));

		for (FieldContext fld : ctx.field())
			lib.getFields().add(visitField(fld));

		for (ClassDefContext cls : ctx.classDef())
			lib.getClasses().add(visitClassDef(cls));

		return lib;
	}

	@Override
	public Pair<Runtime, Collection<Library>> visitFile(
			FileContext ctx) {
		Runtime runtime = new Runtime(locationManager);
		Collection<Library> libraries = new LinkedList<>();

		for (ClassDefContext cls : ctx.classDef())
			runtime.getClasses().add(visitClassDef(cls));
		for (LibraryContext lib : ctx.library())
			libraries.add(visitLibrary(lib));
		for (MethodContext meth : ctx.method()) {
			runtime.getMethods().add(visitMethod(meth));
		}
		return Pair.of(runtime, libraries);
	}

	public static class LibraryParsingException extends RuntimeException {

		private static final long serialVersionUID = 719012473263650111L;

		public LibraryParsingException(
				String file,
				String reason) {
			super("Error while parsing library definitions from " + file + ": " + reason);
		}

		public LibraryParsingException(
				String file,
				Throwable cause) {
			super("Error while parsing library definitions from " + file, cause);
		}
	}

	public static class LibraryCreationException extends RuntimeException {

		private static final long serialVersionUID = 6126702605474874324L;

		public LibraryCreationException() {
			super();
		}

		public LibraryCreationException(
				Throwable cause) {
			super("Creating part of a library", cause);
		}
	}
}
