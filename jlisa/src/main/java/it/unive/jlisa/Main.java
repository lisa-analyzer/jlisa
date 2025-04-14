package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.lisa.program.Program;
import org.eclipse.jdt.core.dom.*;


public class Main {
    public static void main(String[] args) {
        String source = "public class Hello { public static void main() { System.out.println(\"Hello World :)\"); } }";

        ASTParser parser = ASTParser.newParser(AST.getJLSLatest()); // NOTE: JLS8 is deprecated. getJLSLatest will return JDK23
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        Program p = JavaFrontend.parseFromString(source);
        System.out.println(p);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        cu.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                System.out.println("Method: " + node.getName());
                return super.visit(node);
            }
        });
    }
}