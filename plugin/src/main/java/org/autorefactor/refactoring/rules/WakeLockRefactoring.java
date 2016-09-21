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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;

import static org.autorefactor.refactoring.ASTHelper.*;
import static org.eclipse.jdt.core.dom.ASTNode.*;

import org.autorefactor.refactoring.ASTBuilder;
import org.autorefactor.refactoring.Refactorings;

/** See {@link #getDescription()} method. */
public class WakeLockRefactoring extends AbstractRefactoringRule {

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
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
            	TypeDeclaration typeDecl= (TypeDeclaration)ASTNodes.getParent(enclosingMethod, TypeDeclaration.class);
            	MethodDeclaration[] methods = typeDecl.getMethods();
				for(MethodDeclaration method : methods ) {
            	    if("onPause".equals(method.resolveBinding().getName())){
            	    	r.insertAt(
            	    			b.move(node.getParent()),
            	    			method.getBody().statements().size(),
            	    			Block.STATEMENTS_PROPERTY,
            	    			method.getBody()
            	    	);
            	        break;
            	    }
            	}
            }
            // put it on onPause
            return DO_NOT_VISIT_SUBTREE;
        }
        return VISIT_SUBTREE;
    }
}
