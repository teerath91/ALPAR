/*
 * AutoRefactor - Eclipse plugin to automatically refactor Java code bases.
 *
 * Copyright (C) 2013-2016 Jean-Noël Rouvignac - initial API and implementation
 * Copyright (C) 2016 Fabrice Tiercelin - Make sure we do not visit again modified nodes
 * Copyright (C) 2016 Luis Cruz - Android Refactoring
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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;

import com.sun.jndi.cosnaming.RemoteToCorba;

import static org.autorefactor.refactoring.ASTHelper.*;
import static org.eclipse.jdt.core.dom.ASTNode.*;

import java.util.List;

import org.autorefactor.refactoring.ASTBuilder;
import org.autorefactor.refactoring.Refactorings;

/** See {@link #getDescription()} method. */
public class RecycleRefactoring extends AbstractRefactoringRule {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Many resources, such as TypedArrays, VelocityTrackers, etc., should be "
				+ "recycled (with a recycle()/close() call) after use. "
				+ "Inspired from "
				+ "https://android.googlesource.com/platform/tools/base/+/master/lint/libs/lint-checks/src/main/java/com/android/tools/lint/checks/CleanupDetector.java";
	}

	@Override
	public String getName() {
		return "RecycleRefactoring";
	}

    @Override
    public boolean visit(MethodInvocation node) {
		final ASTBuilder b = this.ctx.getASTBuilder();
		final Refactorings r = this.ctx.getRefactorings();
		if(isMethod(
			node,
			"android.database.sqlite.SQLiteDatabase",
			"query", "java.lang.String","java.lang.String[]","java.lang.String","java.lang.String[]", "java.lang.String","java.lang.String","java.lang.String")
		){
//			r.replace(node.getName(), b.simpleName("querby"));
			ClosePresenceChecker closePresenceChecker = new ClosePresenceChecker("closeVariableTODO");
    		Block block = (Block) ASTNodes.getParent(node, ASTNode.BLOCK);
    		block.accept(closePresenceChecker);
    		if(!closePresenceChecker.closePresent){
    			MethodInvocation closeInvocation = b.getAST().newMethodInvocation();
        		closeInvocation.setName(b.simpleName("close"));
        		VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) ASTNodes.getParent(node, ASTNode.VARIABLE_DECLARATION_FRAGMENT);
        		SimpleName cursorExpression = variableDeclarationFragment.getName();
        		closeInvocation.setExpression(b.copy(cursorExpression));
        		ExpressionStatement expressionStatement = b.getAST().newExpressionStatement(closeInvocation);
        		Statement cursorAssignmentExpressionStatement = (Statement) ASTNodes.getParent(node, ASTNode.VARIABLE_DECLARATION_STATEMENT);
        		r.insertAfter(expressionStatement, cursorAssignmentExpressionStatement);
        		return DO_NOT_VISIT_SUBTREE;
    		}
    	}
    	return VISIT_SUBTREE;
    }
    
    public class ClosePresenceChecker extends ASTVisitor {
    	public boolean closePresent;
    	private String cursorVariableName;
    	
    	
    	public ClosePresenceChecker(String cursorVariableName) {
			super();
			this.closePresent = false;
			this.cursorVariableName = cursorVariableName;
		}

		@Override
        public boolean visit(MethodInvocation node) {
    		if(isMethod(node, "android.database.Cursor", "close")){
    			this.closePresent=true;
    			return DO_NOT_VISIT_SUBTREE;
    		}
    		return VISIT_SUBTREE;
    	}
    }

}
