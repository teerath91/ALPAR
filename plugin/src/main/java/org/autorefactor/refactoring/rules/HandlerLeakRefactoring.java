package org.autorefactor.refactoring.rules;

import static org.autorefactor.refactoring.ASTHelper.VISIT_SUBTREE;
import static org.autorefactor.refactoring.ASTHelper.getEnclosingType;

import java.util.List;

import org.autorefactor.refactoring.ASTBuilder;
import org.autorefactor.refactoring.Refactorings;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class HandlerLeakRefactoring extends AbstractRefactoringRule {
	
	CompilationUnit cu;
	boolean flag=true;

	@Override
	public String getDescription() {
		return "Prevents memory leak in Android " + "by refactoring Handler classes";
	}


	@Override
	public String getName() {
		return "HandlerLeakRefactoring";
	}
	
	@Override
	public boolean visit(CompilationUnit node) {
		cu=node;	 
		return VISIT_SUBTREE;
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		if (node.getType().toString().equals("Handler")) {
			if (!node.fragments().isEmpty()) {
				VariableDeclarationFragment varFragment = (VariableDeclarationFragment) node.fragments().get(0);
				ClassInstanceCreation classInstance = (ClassInstanceCreation) varFragment.getInitializer();
				if (classInstance.getAnonymousClassDeclaration() != null) {
					String fullName = varFragment.getName().toString();
					createNewNInnerClass(node, varToTypeName(fullName));
				}
			}
		}
		return VISIT_SUBTREE;
	}

	/**
	 * Creates new inner class to replace Handler anonymous classes
	 * 
	 * @param node
	 * @param handname
	 */
	@SuppressWarnings("unchecked")
	private void createNewNInnerClass(FieldDeclaration node, String handname) {

		final AST ast = node.getAST();
		final TypeDeclaration newDeclaration = ast.newTypeDeclaration();
		final Refactorings r = this.ctx.getRefactorings();
		final ASTBuilder b = this.ctx.getASTBuilder();
		
		String activityName = ((TypeDeclaration)getEnclosingType(node)).getName().toString();
		
		newDeclaration.setInterface(false);
		newDeclaration.setJavadoc(null);
		newDeclaration.modifiers().add(b.getAST().newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		newDeclaration.modifiers().add(b.getAST().newModifier(ModifierKeyword.STATIC_KEYWORD));
		newDeclaration.setName(ast.newSimpleName(handname));

		Name name = ast.newName("Handler");
		Type type = ast.newSimpleType(name);
		newDeclaration.setSuperclassType(type);

		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName("mActivity"));
		FieldDeclaration weakRefFieldDec = ast.newFieldDeclaration(fragment);

		weakRefFieldDec.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		weakRefFieldDec.modifiers().add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));

		Type typee = b.type("WeakReference");
		final ParameterizedType parameterizedType = ast.newParameterizedType(typee);
		Type genericType = ast.newSimpleType(ast.newSimpleName(activityName));
		parameterizedType.typeArguments().add(genericType);
		weakRefFieldDec.setType(parameterizedType);

		final MethodDeclaration constructor = ast.newMethodDeclaration();
		constructor.setConstructor(true);
		constructor.setName(ast.newSimpleName(handname));
		constructor.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		SingleVariableDeclaration methodParam = ast.newSingleVariableDeclaration();
		methodParam.setName(ast.newSimpleName("activity"));
		methodParam.setType(ast.newSimpleType(ast.newSimpleName(activityName)));
		constructor.parameters().add(methodParam);

		final Block constructorBody = ast.newBlock();
		Assignment assignment = ast.newAssignment();
		assignment.setLeftHandSide(ast.newSimpleName("mActivity"));
		ClassInstanceCreation rightHand = ast.newClassInstanceCreation();
		Name name1 = ast.newName("WeakReference");
		Type type1 = ast.newSimpleType(name1);
		final ParameterizedType parameterizedType1 = ast.newParameterizedType(type1);
		Type genericType1 = ast.newSimpleType(ast.newSimpleName(activityName));
		parameterizedType1.typeArguments().add(genericType1);
		rightHand.setType(parameterizedType1);
		rightHand.arguments().add(ast.newSimpleName("activity"));
		assignment.setRightHandSide(rightHand);
		ExpressionStatement expr = ast.newExpressionStatement(assignment);
		constructorBody.statements().add(expr);
		constructor.setBody(constructorBody);

		newDeclaration.bodyDeclarations().add(weakRefFieldDec);
		newDeclaration.bodyDeclarations().add(constructor);

		VariableDeclarationFragment varFragment = (VariableDeclarationFragment) node.fragments().get(0);
		ClassInstanceCreation classInstance = (ClassInstanceCreation) varFragment.getInitializer();

		MethodDeclaration bodyDeclaration =(MethodDeclaration) classInstance.getAnonymousClassDeclaration().bodyDeclarations().get(0);
		MethodDeclaration[] m= newDeclaration.getMethods();
		
		Type handlerType =  ast.newSimpleType(ast.newSimpleName(handname));
		r.replace(node.getType(), handlerType);
		
		ClassInstanceCreation newHandler = ast.newClassInstanceCreation();
		newHandler.setType(handlerType);
		newHandler.arguments().add(b.this0());
		r.replace(classInstance, newHandler);
		
		InfixExpression newCondition= ast.newInfixExpression();
		newCondition.setOperator(Operator.NOT_EQUALS);
		newCondition.setLeftOperand(ast.newSimpleName(activityName));
		newCondition.setRightOperand(ast.newNullLiteral());
		
		IfStatement ifStatement= ast.newIfStatement();
		ifStatement.setExpression(newCondition);
		Block newBlock1 = bodyDeclaration.getBody();
		final Block ifblock = ast.newBlock();
		ifblock.statements().add(ifStatement);
		r.replace(newBlock1, ifblock);
		r.replace(ifStatement.getThenStatement(), bodyDeclaration.getBody());
		
		VariableDeclarationFragment newfragment = ast.newVariableDeclarationFragment();
		newfragment.setName(ast.newSimpleName("activity"));
		
		VariableDeclarationExpression leftsideactivity = ast.newVariableDeclarationExpression(newfragment);
		leftsideactivity.setType(ast.newSimpleType(ast.newSimpleName(activityName)));
		Assignment assignmentactivity = ast.newAssignment();
		
		final MethodInvocation methodInvocation = ast.newMethodInvocation();
		final Name name4 = ast.newSimpleName("mActivity");
		methodInvocation.setExpression(name4);
		methodInvocation.setName(ast.newSimpleName("get"));
		assignmentactivity.setLeftHandSide(leftsideactivity);
		assignmentactivity.setRightHandSide(methodInvocation);
		ExpressionStatement expr1 = ast.newExpressionStatement(assignmentactivity);
		r.insertBefore(expr1, ifStatement);
		
		List<ImportDeclaration> lstImports = cu.imports();

		AST ast1 = cu.getAST();
		ImportDeclaration id = ast1.newImportDeclaration();
		id.setName(ast1.newName(new String[] { "java", "lang", "ref","WeakReference" }));

		for(ImportDeclaration importdec:lstImports) {
			String imp=importdec.toString().trim();
			if(imp.equalsIgnoreCase("import java.lang.ref.WeakReference;"))
				flag=false;
		}
		  if(lstImports.size()>0 && flag) { 
			  
		  r.insertAfter(id, lstImports.get(0));
		  flag=false;
		  }
		r.insertBefore(newDeclaration.getRoot(), node);
		r.insertAfter(bodyDeclaration, m[0]);
	}
	
	/**
	 * Methods to get new static class name from variable name.
	 * 
	 * @param varName
	 * @return
	 */
	private String varToTypeName(String varName) {
		return varName.concat("1");
	}
}