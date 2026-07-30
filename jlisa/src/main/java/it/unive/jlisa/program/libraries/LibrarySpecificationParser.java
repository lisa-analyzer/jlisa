package it.unive.jlisa.program.libraries;

import it.unive.jlisa.antlr.LibraryDefinitionParser;
import it.unive.jlisa.antlr.LibraryDefinitionParser.ClassDefContext;
import it.unive.jlisa.antlr.LibraryDefinitionParser.FieldContext;
import it.unive.jlisa.antlr.LibraryDefinitionParser.FileContext;
import it.unive.jlisa.antlr.LibraryDefinitionParser.LibtypeContext;
import it.unive.jlisa.antlr.LibraryDefinitionParser.LisatypeContext;
import it.unive.jlisa.antlr.LibraryDefinitionParser.MethodContext;
import it.unive.jlisa.antlr.LibraryDefinitionParser.ParamContext;
import it.unive.jlisa.antlr.LibraryDefinitionParser.TypeContext;
import it.unive.jlisa.antlr.LibraryDefinitionParserBaseVisitor;
import it.unive.jlisa.program.SourceCodeLocationManager;
import it.unive.jlisa.program.libraries.loader.BooleanValue;
import it.unive.jlisa.program.libraries.loader.ClassDef;
import it.unive.jlisa.program.libraries.loader.Field;
import it.unive.jlisa.program.libraries.loader.LiSAType;
import it.unive.jlisa.program.libraries.loader.LibType;
import it.unive.jlisa.program.libraries.loader.Method;
import it.unive.jlisa.program.libraries.loader.NoneValue;
import it.unive.jlisa.program.libraries.loader.NumberValue;
import it.unive.jlisa.program.libraries.loader.Parameter;
import it.unive.jlisa.program.libraries.loader.Runtime;
import it.unive.jlisa.program.libraries.loader.StringValue;
import it.unive.jlisa.program.libraries.loader.Type;
import it.unive.jlisa.program.libraries.loader.Value;

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
				locationManager,
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
		boolean instance = ctx.INSTANCE() != null;
		String name = ctx.name.getText();
		Type type = visitType(ctx.type());
		if (ctx.DEFAULT() == null)
			return new Field(instance, name, type);

		Value def = getDefaultValue(ctx.val);
		if (def == null)
			throw new LibraryParsingException(file, "Unsupported default field type: " + type);

		return new Field(instance, name, type, def);
	}

	@Override
	public Parameter visitParam(
			ParamContext ctx) {
		Type type = visitType(ctx.type());
		String name = ctx.name.getText();
		if (ctx.DEFAULT() == null)
			return new Parameter(name, type);

		Value def = getDefaultValue(ctx.val);
		if (def == null)
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
	public Runtime visitFile(
			FileContext ctx) {
		Runtime runtime = new Runtime(locationManager);

		for (ClassDefContext cls : ctx.classDef())
			runtime.getClasses().add(visitClassDef(cls));
		for (FieldContext fld : ctx.field())
			runtime.getFields().add(visitField(fld));
		for (MethodContext meth : ctx.method())
			runtime.getMethods().add(visitMethod(meth));
		return runtime;
	}

	private Value getDefaultValue(
			LibraryDefinitionParser.ValueContext val) {
		if (val.NONE() != null)
			return new NoneValue();
		else if (val.BOOLEAN() != null)
			return new BooleanValue(val.BOOLEAN().getText().equals("true"));
		else if (val.STRING() != null)
			return new StringValue(val.STRING().getText());
		else if (val.NUMBER() != null)
			return new NumberValue(Integer.parseInt(val.NUMBER().getText()));
		else
			return null;
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
