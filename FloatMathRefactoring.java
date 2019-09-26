package org.autorefactor.refactoring.rules;

import static org.autorefactor.refactoring.ASTHelper.DO_NOT_VISIT_SUBTREE;
import static org.autorefactor.refactoring.ASTHelper.VISIT_SUBTREE;

import org.autorefactor.refactoring.ASTBuilder;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

public class FloatMathRefactoring extends AbstractRefactoringRule{
	@Override
	public String getName() {
		return "FloatMathRefactoring";
	}
	
	@Override
	public String getDescription() {
		return "test description";
	}
	
    @Override
    public boolean visit(MethodInvocation node) {
    	if(node.getExpression()!=null && "FloatMath".equals(node.getExpression().toString())) {
    		refactorTypeCast(node);
    		return DO_NOT_VISIT_SUBTREE;
    	}
    	return VISIT_SUBTREE;
    }

	private boolean refactorTypeCast(MethodInvocation node) {
		final ASTBuilder b = this.ctx.getASTBuilder();
		AST ast = b.getAST();
		SimpleName name = ast.newSimpleName("Math");
		CastExpression castExpr = ast.newCastExpression();
		castExpr.setType(b.type("float"));
		castExpr.setExpression(name);
		this.ctx.getRefactorings().replace(node.getExpression(), castExpr);
		return DO_NOT_VISIT_SUBTREE;
	}
    
}
