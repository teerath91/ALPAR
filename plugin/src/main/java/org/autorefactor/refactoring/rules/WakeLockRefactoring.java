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
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;

import static org.autorefactor.refactoring.ASTHelper.*;
import static org.eclipse.jdt.core.dom.ASTNode.*;

import java.util.List;

import org.autorefactor.refactoring.ASTBuilder;
import org.autorefactor.refactoring.Refactorings;

/** See {@link #getDescription()} method. */
public class WakeLockRefactoring extends AbstractRefactoringRule {

	@Override
	public String getDescription() {
		return "Failing to release a wakelock properly can keep the Android device in a high "
				+ "power mode, which reduces battery life. There are several causes of this, such "
				+ "as releasing the wake lock in onDestroy() instead of in onPause(), failing to "
				+ "call release() in all possible code paths after an acquire(), and so on.";
	}

	@Override
	public String getName() {
		return "WakeLockRefactoring";
	}

    @Override
    public boolean visit(MethodInvocation node) {
    	if(isMethod(node, "android.os.PowerManager.WakeLock", "release")){
    		// check whether it is being called in onDestroy
    		final Refactorings r = this.ctx.getRefactorings();
    		final ASTBuilder b = this.ctx.getASTBuilder();
    		MethodDeclaration enclosingMethod = (MethodDeclaration) ASTNodes.getParent(node, ASTNode.METHOD_DECLARATION);
    		if(isMethod(enclosingMethod.resolveBinding(), "android.app.Activity", "onDestroy")){
    			TypeDeclaration typeDeclaration= (TypeDeclaration)ASTNodes.getParent(enclosingMethod, TypeDeclaration.class);
    			MethodDeclaration onPauseMethod = findMethodOfType("onPause", typeDeclaration);
    			if(onPauseMethod != null && node.getParent().getNodeType() == ASTNode.EXPRESSION_STATEMENT){
    				Statement releaseNode = createWakelockReleaseNode(node);				
    				r.remove(node.getParent());
    				r.insertAt(releaseNode,
    						onPauseMethod.getBody().statements().size(),
    						Block.STATEMENTS_PROPERTY,
    						onPauseMethod.getBody());
    				return DO_NOT_VISIT_SUBTREE;
    			}
    			/* If it reaches this part of the code, it
    			 * means it did not find onPause method in the class.
    			 */
    			MethodDeclaration onPauseDeclaration = createOnPauseDeclaration();

    			// add onPause declaration to the Activity
    			r.insertAfter(onPauseDeclaration, enclosingMethod);				
    			return DO_NOT_VISIT_SUBTREE;

    		}
    	}
    	else if(isMethod(node, "android.os.PowerManager.WakeLock", "acquire")){
    		final Refactorings r = this.ctx.getRefactorings();
    		final ASTBuilder b = this.ctx.getASTBuilder();
    		TypeDeclaration typeDeclaration= (TypeDeclaration) ASTNodes.getParent(node, ASTNode.TYPE_DECLARATION);
    		ReleasePresenceChecker releasePresenceChecker = new ReleasePresenceChecker();
    		typeDeclaration.accept(releasePresenceChecker);
			if(!releasePresenceChecker.releasePresent){
				Statement releaseNode = createWakelockReleaseNode(node);				
    			MethodDeclaration onPauseMethod = findMethodOfType("onPause", typeDeclaration);
    			if(onPauseMethod != null && node.getParent().getNodeType() == ASTNode.EXPRESSION_STATEMENT){
    				r.insertAt(releaseNode,
    						onPauseMethod.getBody().statements().size(),
    						Block.STATEMENTS_PROPERTY,
    						onPauseMethod.getBody());
    				return DO_NOT_VISIT_SUBTREE;
    			}
    			else{
    				MethodDeclaration onPauseDeclaration = createOnPauseDeclaration();
    				/* TODO @JnRouvignac, instead of using insertAt, calling 
    				 * typeDeclaration.bodyDeclarations().add(onPauseDeclaration);
    				 * would be more intuitive.
    				 */
    				r.insertAt(
    						onPauseDeclaration,
    						typeDeclaration.bodyDeclarations().size(),
    						typeDeclaration.getBodyDeclarationsProperty(),
    						typeDeclaration
    				);
    				return DO_NOT_VISIT_SUBTREE;
    			}
			}
    	}
    	return VISIT_SUBTREE;
    }
    
    private Statement createWakelockReleaseNode(MethodInvocation methodInvocation){
    	final ASTBuilder b = this.ctx.getASTBuilder();
    	IfStatement ifStatement = b.getAST().newIfStatement();
    	// test-clause
    	PrefixExpression prefixExpression = b.getAST().newPrefixExpression();
    	prefixExpression.setOperator(PrefixExpression.Operator.NOT);
    	MethodInvocation isHeldInvocation = b.getAST().newMethodInvocation();
		isHeldInvocation.setName(b.simpleName("isHeld"));
		isHeldInvocation.setExpression(b.copyExpression(methodInvocation));
		prefixExpression.setOperand(isHeldInvocation);
		ifStatement.setExpression(prefixExpression);
		// then
		MethodInvocation releaseInvocation = b.getAST().newMethodInvocation();
		releaseInvocation.setName(b.simpleName("release"));
		releaseInvocation.setExpression(b.copyExpression(methodInvocation));
		ExpressionStatement releaseExpressionStatement = b.getAST().newExpressionStatement(releaseInvocation);
		ifStatement.setThenStatement(b.block(releaseExpressionStatement));
		return ifStatement;
    }

	private MethodDeclaration createOnPauseDeclaration() {
		final ASTBuilder b = this.ctx.getASTBuilder();
		MethodDeclaration onPauseDeclaration = b.getAST().newMethodDeclaration();
		onPauseDeclaration.setName(b.simpleName("onPause"));
		//
		NormalAnnotation annotation = b.getAST().newNormalAnnotation();
		annotation.setTypeName(b.name("Override"));
		onPauseDeclaration.modifiers().add(annotation);
		//
		Modifier protectedModifier = b.getAST().newModifier(ModifierKeyword.PROTECTED_KEYWORD);
		onPauseDeclaration.modifiers().add(protectedModifier);
		//
		SuperMethodInvocation superMethodInvocation = b.getAST().newSuperMethodInvocation();
		superMethodInvocation.setName(b.simpleName("onPause"));

		//
		ASTRewrite rewriter = ASTRewrite.create(b.getAST());
		onPauseDeclaration.setBody(b.block(b.newlinePlaceholder(),b.getAST().newExpressionStatement(superMethodInvocation), b.newlinePlaceholder()));
		return onPauseDeclaration;
	}

	private MethodDeclaration findMethodOfType(String methodToFind,
			TypeDeclaration typeDeclaration) {
		MethodDeclaration[] methods = typeDeclaration.getMethods();
		
		for(MethodDeclaration method : methods ) {
			IMethodBinding methodBinding = method.resolveBinding();
								if(
				methodBinding != null
				&& methodToFind.equals(methodBinding.getName())
			){
				return method;
			}
		}
		return null;
	}
    
    
    public class ReleasePresenceChecker extends ASTVisitor {
    	public boolean releasePresent = false;
    	@Override
        public boolean visit(MethodInvocation node) {
    		if(isMethod(node, "android.os.PowerManager.WakeLock", "release")){
    			this.releasePresent=true;
    			return DO_NOT_VISIT_SUBTREE;
    		}
    		return VISIT_SUBTREE;
    	}
    }
}
