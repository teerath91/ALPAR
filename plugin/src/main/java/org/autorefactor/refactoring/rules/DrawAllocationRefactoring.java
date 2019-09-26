/*
 * AutoRefactor - Eclipse plugin to automatically refactor Java code bases.
 *
 * Copyright (C) 2013-2016 Jean-Noël Rouvignac - initial API and implementation
 * Copyright (C) 2016 Fabrice Tiercelin - Make sure we do not visit again modified nodes
 * Copyright (C) 2016 Luis Cruz - Android Refactoring Rules
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program under LICENSE-GNUGPL.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution under LICENSE-ECLIPSE, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.autorefactor.refactoring.rules;

import static org.autorefactor.refactoring.ASTHelper.DO_NOT_VISIT_SUBTREE;
import static org.autorefactor.refactoring.ASTHelper.VISIT_SUBTREE;
import static org.autorefactor.refactoring.ASTHelper.isMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.autorefactor.refactoring.ASTBuilder;
import org.autorefactor.refactoring.Refactorings;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;

/** See {@link #getDescription()} method. */
public class DrawAllocationRefactoring extends AbstractRefactoringRule {

	@Override
	public String getDescription() {
		return "Optimization for Android applications to avoid the allocation of"
				+ " objects inside drawing routines. ";
	}

	@Override
	public String getName() {
		return "DrawAllocationRefactoring";
	}

	/**
	 * Method that determines when a method invocation is eligible for refactoring
	 * 
	 * @param methodBinding
	 * @return
	 */
	public boolean isMethodEligible(IMethodBinding methodBinding) {
		if( isMethod(
				methodBinding,
				"android.widget.TextView",
				"onDraw",
				"android.graphics.Canvas"
		)|| isMethod(
				methodBinding,
				"android.view.View",
				"onDraw",
				"android.graphics.Canvas"
		)|| isMethod(
				methodBinding,
				"android.widget.TextView",
				"onMeasure",
				"int","int"
		)|| isMethod(
				methodBinding,
				"android.widget.LinearLayout",
				"onMeasure",
				"int","int"
		)|| isMethod(
				methodBinding,
				"android.widget.LinearLayout",
				"onLayout",
				"boolean","int","int","int","int"
		)|| isMethod(
				methodBinding,
				"android.widget.SeekBar",
				"onDraw",
				"android.graphics.Canvas"
		)|| isMethod(
				methodBinding,
				"android.widget.EditText",
				"onDraw",
				"android.graphics.Canvas"
		)|| isMethod(
				methodBinding,
				"android.view.SurfaceView",
				"onDraw",
				"android.graphics.Canvas"
		)) return true;
		return false;
	}
	
	public boolean visit(MethodDeclaration node) {
		IMethodBinding methodBinding = node.resolveBinding();
		if(methodBinding!= null && isMethodEligible(methodBinding)){
			node.accept(new OnDrawTransformer(this.ctx, node));
		}
		return VISIT_SUBTREE;
	}
	
	static class OnDrawTransformer extends ASTVisitor{
		private RefactoringContext ctx;
		private MethodDeclaration methodDeclaration;
		private static ArrayList<String> rectClassList = new ArrayList<String>();
		static {
			rectClassList.add("Rect");
			rectClassList.add("RectF");
		}
		
		public OnDrawTransformer(RefactoringContext ctx, MethodDeclaration methodDeclaration){
			this.ctx=ctx;
			this.methodDeclaration = methodDeclaration;
		}
		
		public boolean isMethodBindingSubclassOf(ITypeBinding typeBinding, List<String> superClassStrings){
			ITypeBinding superClass = typeBinding;
			while(superClass!= null && !superClass.equals(ctx.getAST().resolveWellKnownType("java.lang.Object"))){
				String className = superClass.getName();
				if(className.contains("<")){
					className = className.split("<",2)[0];
				}
				if(superClassStrings.contains(className)){
					return true;
				}
				superClass = superClass.getSuperclass();
			}
			return false;
		}
		
		@Override
		public boolean visit(VariableDeclarationFragment node) {
			
			final ASTBuilder b = this.ctx.getASTBuilder();
			final Refactorings r = this.ctx.getRefactorings();
			
			Expression initializer = node.getInitializer();
			if(initializer != null){
				if(initializer.getNodeType() == ASTNode.CAST_EXPRESSION){
					initializer = ((CastExpression)initializer).getExpression();
				}
				if(initializer.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION){
					ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) initializer;
					InitializerVisitor initializerVisitor = new InitializerVisitor();
					initializer.accept(initializerVisitor);
					if(initializerVisitor.initializerCanBeExtracted){
						Statement declarationStatement = (Statement) ASTNodes.getParent(node, ASTNode.VARIABLE_DECLARATION_STATEMENT);
						if(declarationStatement!= null){
							//Deal with collections
							if(
								isMethodBindingSubclassOf(
									node.getName().resolveTypeBinding(),
									Arrays.asList("AbstractCollection", "Collection","List", "AbstractMap", "Map")
								)
							){
								if(classInstanceCreation.arguments().size() == 0){
									r.insertBefore(b.move(declarationStatement), methodDeclaration);
									ASTNode clearNode = b.getAST().newExpressionStatement(
										b.invoke(node.getName().getIdentifier(), "clear")
									);
									List bodyStatements = methodDeclaration.getBody().statements();
									Statement lastStatement = (Statement) bodyStatements.get(bodyStatements.size() - 1);
									int whereToInsertClearStatement = bodyStatements.size();
									if(ASTNode.RETURN_STATEMENT == lastStatement.getNodeType()){
										whereToInsertClearStatement -= 1;
									}
									r.insertAt(clearNode, whereToInsertClearStatement, Block.STATEMENTS_PROPERTY, methodDeclaration.getBody());
								}
							}
							else{
								r.insertBefore(b.move(declarationStatement), methodDeclaration);									
							}
						}
					} else if(rectClassList.contains(classInstanceCreation.getType().toString())) {
						Statement declarationStatement = (Statement) ASTNodes.getParent(node, ASTNode.VARIABLE_DECLARATION_STATEMENT);
						if(declarationStatement != null && classInstanceCreation.arguments().size()==4) {
							@SuppressWarnings("unchecked")
							List<Expression> argList = classInstanceCreation.arguments();
							AST ast = classInstanceCreation.getAST();
							ClassInstanceCreation cNode = ast.newClassInstanceCreation();
							cNode.setType(ast.newSimpleType(ast.newName(classInstanceCreation.getType().toString())));
							r.replace(classInstanceCreation, cNode);
							
							insertRectAssignments(node, argList);
							
							r.insertBefore(b.move(declarationStatement), methodDeclaration);
						}
					}
				}
			}
			return DO_NOT_VISIT_SUBTREE;
		}
		
		@Override
		public boolean visit(MethodInvocation node) {
			final Refactorings r = this.ctx.getRefactorings();
			AST ast = node.getAST();
			String methodName = node.getName().toString();
			Expression methodExpression = node.getExpression();
			if(methodExpression == null)
				return DO_NOT_VISIT_SUBTREE;
			String methodExpressionString = methodExpression.toString();
			node.getClass();
			if(isMethod(
					(IMethodBinding)node.getName().resolveBinding(),
					"android.graphics.Canvas",
					"drawRect",
					"android.graphics.Rect", "android.graphics.Paint"
			)||isMethod(
					(IMethodBinding)node.getName().resolveBinding(),
					"android.graphics.Canvas",
					"drawRect",
					"android.graphics.RectF", "android.graphics.Paint"
			)){
				List<Expression> argList = node.arguments();
				if(argList.size()==2) {
					Expression expr = argList.get(0);
					if(expr instanceof ClassInstanceCreation) {
						String classType = ((ClassInstanceCreation) expr).getType().toString();
						if(rectClassList.contains(classType)) {
							List<Expression> tempArgList = ((ClassInstanceCreation) expr).arguments();
							MethodInvocation newNode = ast.newMethodInvocation();
							Name exprName = ast.newSimpleName(methodExpressionString);
							newNode.setExpression(exprName);
							newNode.setName(ast.newSimpleName(methodName));
							for(Expression e: tempArgList) {
								newNode.arguments().add(r.createCopyTarget(e));
							}
							newNode.arguments().add(r.createCopyTarget(argList.get(1)));
							r.replace(node, newNode);
						}
					}
				}
			}
			return DO_NOT_VISIT_SUBTREE;
		}
		
		public void insertRectAssignments(VariableDeclarationFragment node, List<Expression> argList) {
			if(node==null || argList.size()!=4)
				return;
			final ASTBuilder b = this.ctx.getASTBuilder();
			final Refactorings r = this.ctx.getRefactorings();
			QualifiedName qName = null;
			AST ast = node.getAST();
			String[] argName = {"left","top","right","bottom"};
			for(int i = 0; i<4; i++) {
				qName = ast.newQualifiedName(ast.newName(node.getName().toString()), ast.newSimpleName(argName[i]));
				Statement stmt = ast.newExpressionStatement(b.assign(b.copy(qName), Operator.ASSIGN, b.copy(argList.get(i))));
				r.insertAfter(stmt,node.getParent());
			}
	 	}
	}
	
	static class InitializerVisitor extends ASTVisitor{
		public boolean initializerCanBeExtracted=true;
		
		@Override
		public boolean visit(MethodInvocation node) {
			initializerCanBeExtracted=false;
			return DO_NOT_VISIT_SUBTREE;
		}
		
		@Override
		public boolean visit(SimpleType node) {
			return DO_NOT_VISIT_SUBTREE;
		}
		
		@Override
		public boolean visit(SimpleName node) {
			initializerCanBeExtracted=false;
			return DO_NOT_VISIT_SUBTREE;
		}
	}    
}