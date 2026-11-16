package it.unive.jlisa.frontend.util;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class LoopUnrolling {

	public static boolean canUnroll(
			WhileStatement node) {
		Checker checker = new Checker();
		node.getBody().accept(checker);
		return checker.ok;
	}

	public static boolean canUnroll(
			EnhancedForStatement node) {
		Checker checker = new Checker();
		node.getBody().accept(checker);
		return checker.ok;
	}

	private static class Checker extends ASTVisitor {

		boolean ok = true;

		private boolean reject() {
			ok = false;
			return false;
		}

		@Override
		public boolean visit(
				Block node) {
			return true;
		}

		@Override
		public boolean visit(
				BreakStatement node) {
			return reject();
		}

		@Override
		public boolean visit(
				ContinueStatement node) {
			return reject();
		}

		@Override
		public boolean visit(
				LabeledStatement node) {
			return reject();
		}

		@Override
		public boolean visit(
				WhileStatement node) {
			return reject();
		}

		@Override
		public boolean visit(
				DoStatement node) {
			return reject();
		}

		@Override
		public boolean visit(
				ForStatement node) {
			return reject();
		}

		@Override
		public boolean visit(
				EnhancedForStatement node) {
			return reject();
		}

		@Override
		public boolean visit(
				AnonymousClassDeclaration node) {
			return reject();
		}

		@Override
		public boolean visit(
				LambdaExpression node) {
			return reject();
		}

		@Override
		public boolean visit(
				TypeDeclarationStatement node) {
			return reject();
		}

		@Override
		public boolean visit(
				TryStatement node) {
			return reject();
		}
	}

}
