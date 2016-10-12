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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;

import com.sun.jndi.cosnaming.RemoteToCorba;

import static org.autorefactor.refactoring.ASTHelper.*;
import static org.eclipse.jdt.core.dom.ASTNode.*;

import java.util.List;

import org.autorefactor.refactoring.ASTBuilder;
import org.autorefactor.refactoring.ASTHelper;
import org.autorefactor.refactoring.Refactorings;
import org.autorefactor.refactoring.rules.RecycleRefactoring.VisitorDecorator.NopVisitor;

/* 
 * TODO when the last use of resource is as arg of a method invocation,
 * it should be assumed that the given method will take care of the release.  
 * TODO Track local variables. E.g., when a TypedArray a is assigned to variable b,
 * release() should be called only in one variable. 
 * TODO (low priority) check whether resources are being used after release.
 * TODO add support for FragmentTransaction.beginTransaction(). It can use method
 * chaining (which means local variable might not be present) and it can be released
 * by two methods: commit() and commitAllowingStateLoss()
 */

/** See {@link #getDescription()} method. */
public class ViewHolderRefactoring extends AbstractRefactoringRule {

	@Override
	public String getDescription() {
		return "Optimization for Android applications to optimize getView routines. "
				+ "It allows reducing the calls to inflate and getViewById Android "
				+ "API methods.";
	}

	@Override
	public String getName() {
		return "ViewHolderRefactoring";
	}
	
	
    @Override
    public boolean visit(MethodDeclaration node) {
		final ASTBuilder b = this.ctx.getASTBuilder();
		final Refactorings r = this.ctx.getRefactorings();
		IMethodBinding methodBinding = node.resolveBinding();
		
		if(methodBinding!= null &&
			isMethod(
					methodBinding,
					"android.widget.Adapter",
					"getView",
					"int","android.view.View","android.view.ViewGroup"
			)
		){
			GetViewVisitor visitor = new GetViewVisitor();
			Block body = node.getBody();
			if(body != null){
				body.accept(visitor);
				if(!visitor.usesConvertView &&
					visitor.viewVariable != null &&
					!visitor.isInflateInsideIf()){
					// Transform tree
					
					//Create If statement
			    	IfStatement ifStatement = b.getAST().newIfStatement();
			    	// test-clause
			    	InfixExpression infixExpression = b.getAST().newInfixExpression();
			    	infixExpression.setOperator(InfixExpression.Operator.EQUALS);
			    	infixExpression.setLeftOperand(b.simpleName("convertView"));
			    	infixExpression.setRightOperand(b.getAST().newNullLiteral());
					ifStatement.setExpression(infixExpression);
					//then
					Assignment assignment = b.assign(b.simpleName("convertView"), Assignment.Operator.ASSIGN, b.copy(visitor.getInflateExpression()));
					ifStatement.setThenStatement(b.block(b.getAST().newExpressionStatement(assignment)));
					r.insertBefore(ifStatement, visitor.viewAssignmentStatement);
					
					// assign to local view variable when necessary
					if(!"convertView".equals(visitor.viewVariable.getIdentifier())){
						Statement assignConvertViewToView = null;
						if(visitor.viewVariableDeclarationFragment != null){
							assignConvertViewToView = b.declare(visitor.viewVariable.resolveTypeBinding().getName(), b.copy(visitor.viewVariable), b.simpleName("convertView"));
						}
						else if(visitor.viewVariableAssignment != null){
							assignConvertViewToView = b.getAST().newExpressionStatement(b.assign(b.copy(visitor.viewVariable), Assignment.Operator.ASSIGN, b.simpleName("convertView")));
						}
						if(assignConvertViewToView != null){
							r.insertBefore(assignConvertViewToView, visitor.viewAssignmentStatement);
						}
					}
					r.remove(visitor.viewAssignmentStatement);
					// make sure method returns the view to be reused
					if(visitor.returnStatement!=null){
						r.insertAfter(b.return0(b.copy(visitor.viewVariable)), visitor.returnStatement);
						r.remove(visitor.returnStatement);
					}
					return DO_NOT_VISIT_SUBTREE;
				}
			}
		}
    	return VISIT_SUBTREE;
    }
    
    
	public class GetViewVisitor extends ASTVisitor {
		public boolean usesConvertView= false;
		public SimpleName viewVariable = null;
		public Statement viewAssignmentStatement; 
		public VariableDeclarationFragment viewVariableDeclarationFragment = null;
		public Assignment viewVariableAssignment = null;
		public  ReturnStatement returnStatement = null;
		GetViewVisitor(){
		}
		
		@Override
        public boolean visit(SimpleName node) {
			if (node.getIdentifier() == "convertView" &&
				ASTNodes.getParent(node, ASTNode.RETURN_STATEMENT) == null
			){
				this.usesConvertView = true;
				return DO_NOT_VISIT_SUBTREE;
			}
			return VISIT_SUBTREE;
		}
		
		public boolean visit(MethodInvocation node) {
			if(isMethod(node, "android.view.LayoutInflater", "inflate", "int", "android.view.ViewGroup")){
				this.viewVariableDeclarationFragment = (VariableDeclarationFragment) ASTNodes.getParent(node, ASTNode.VARIABLE_DECLARATION_FRAGMENT);
				if(viewVariableDeclarationFragment!=null){
					this.viewVariable = viewVariableDeclarationFragment.getName();
					this.viewAssignmentStatement = (Statement) ASTNodes.getParent(viewVariableDeclarationFragment, ASTNode.VARIABLE_DECLARATION_STATEMENT);
				}
				else{
					this.viewVariableAssignment = (Assignment) ASTNodes.getParent(node, ASTNode.ASSIGNMENT);
					if(viewVariableAssignment!=null){
						this.viewVariable = (SimpleName) viewVariableAssignment.getLeftHandSide();
						this.viewAssignmentStatement = (ExpressionStatement) ASTNodes.getParent(viewVariableAssignment, ASTNode.EXPRESSION_STATEMENT);
					}
				}
				return DO_NOT_VISIT_SUBTREE;
			}
			return VISIT_SUBTREE;
		}
		
		public boolean visit(ReturnStatement node) {
			this.returnStatement = node;
			return VISIT_SUBTREE;
		}
		
		public boolean isInflateInsideIf(){
			if(this.viewAssignmentStatement != null){
				return ASTNodes.getParent(this.viewAssignmentStatement, ASTNode.IF_STATEMENT) != null;
			}
			return false;
		}
		
		public Expression getInflateExpression(){
			if(this.viewVariableDeclarationFragment != null){
				return this.viewVariableDeclarationFragment.getInitializer();
			}
			else if(this.viewVariableAssignment != null){
				return this.viewVariableAssignment.getRightHandSide();
			}
			return null;
		}
		
	}

    
}
