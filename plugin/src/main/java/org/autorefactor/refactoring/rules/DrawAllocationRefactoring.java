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
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;

import static org.autorefactor.refactoring.ASTHelper.*;
import static org.eclipse.jdt.core.dom.ASTNode.*;

import java.util.LinkedList;
import java.util.List;

import org.autorefactor.refactoring.ASTBuilder;
import org.autorefactor.refactoring.Refactorings;

/* 
 * TODO when findViewById is reusing a local variable,
 * the viewholderitem will create a new field with duplicate name.
 * Possible solution: use the id names instead of var names
 */

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
	
	public boolean visit(MethodDeclaration node) {
		IMethodBinding methodBinding = node.resolveBinding();
		if(methodBinding!= null &&
			isMethod(
					methodBinding,
					"android.widget.TextView",
					"onDraw",
					"android.graphics.Canvas"
			)
		){
			final ASTBuilder b = this.ctx.getASTBuilder();
			final Refactorings r = this.ctx.getRefactorings();
			node.accept(new OnDrawTransformer(this.ctx, node));
		}
		return VISIT_SUBTREE;
	}
	
	static class OnDrawTransformer extends ASTVisitor{
		private RefactoringContext ctx;
		private MethodDeclaration onDrawDeclaration;
		
		public OnDrawTransformer(RefactoringContext ctx, MethodDeclaration onDrawDeclaration){
			this.ctx=ctx;
			this.onDrawDeclaration = onDrawDeclaration;
		}
		@Override
		public boolean visit(VariableDeclarationFragment node) {
			
			final ASTBuilder b = this.ctx.getASTBuilder();
			final Refactorings r = this.ctx.getRefactorings();
			
			Expression initializer = node.getInitializer();
			if(initializer != null){
				if(initializer.getNodeType() == ASTNode.CLASS_INSTANCE_CREATION){ 
					Statement declarationStatement = (Statement) ASTNodes.getParent(node, ASTNode.VARIABLE_DECLARATION_STATEMENT);
					if(declarationStatement!= null){
						r.insertBefore(b.move(declarationStatement), onDrawDeclaration);						
					}
				}
			}
			return VISIT_SUBTREE;
		}
	}
    
    
}
