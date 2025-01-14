package org.autorefactor.refactoring.rules;

import static org.autorefactor.refactoring.ASTHelper.DO_NOT_VISIT_SUBTREE;
import static org.autorefactor.refactoring.ASTHelper.VISIT_SUBTREE;
import static org.autorefactor.refactoring.ASTHelper.arg0;
import static org.autorefactor.refactoring.ASTHelper.arguments;
import static org.autorefactor.refactoring.ASTHelper.hasType;
import static org.autorefactor.refactoring.ASTHelper.isField;
import static org.autorefactor.refactoring.ASTHelper.isMethod;
import static org.autorefactor.refactoring.ASTHelper.isPrimitive;
import static org.autorefactor.refactoring.ASTHelper.removeParentheses;
import static org.autorefactor.refactoring.ASTHelper.resolveTypeBinding;
import static org.autorefactor.util.Utils.*;

import java.util.List;

import org.autorefactor.refactoring.ASTBuilder;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class UseValueOfRefactoring extends AbstractRefactoringRule{

	@Override
	public String getDescription() {
		return ""
			+ "Does magic";
	}

	@Override
	public String getName() {
		return "UseValueOfRefactoring";
	}
	
	private int getJavaMinorVersion() {
        return ctx.getJavaProjectOptions().getJavaSERelease().getMinorVersion();
    }

	/*@Override
	public boolean visit(MethodInvocation node) {
		if(node.getExpression()==null) {
			return VISIT_SUBTREE;
		}
		
		final ASTNode parent = removeParentheses(node.getParent());
		
		return DO_NOT_VISIT_SUBTREE;
	}*/
	
	@Override
    public boolean visit(MethodInvocation node) {
        if (node.getExpression() == null) {
            return VISIT_SUBTREE;
        }

        final ASTNode parent = removeParentheses(node.getParent());
        if (parent instanceof VariableDeclarationFragment) {
            final ITypeBinding typeBinding = resolveTypeBinding((VariableDeclarationFragment) parent);
            if (typeBinding.isPrimitive()
                    && "valueOf".equals(node.getName().getIdentifier())) {
                if (isMethod(node, "java.lang.Boolean", "valueOf", "boolean")
                        || isMethod(node, "java.lang.Byte", "valueOf", "byte")
                        || isMethod(node, "java.lang.Character", "valueOf", "char")
                        || isMethod(node, "java.lang.Short", "valueOf", "short")
                        || isMethod(node, "java.lang.Integer", "valueOf", "int")
                        || isMethod(node, "java.lang.Long", "valueOf", "long")
                        || isMethod(node, "java.lang.Float", "valueOf", "float")
                        || isMethod(node, "java.lang.Double", "valueOf", "double")) {
                    return replaceWithTheSingleArgument(node);
                }
                if (is(node, "java.lang.Byte")) {
                    return replaceMethodName(node, "parseByte");
                }
                if (is(node, "java.lang.Short")) {
                    return replaceMethodName(node, "parseShort");
                }
                if (is(node, "java.lang.Integer")) {
                    return replaceMethodName(node, "parseInt");
                }
                if (is(node, "java.lang.Long")) {
                    return replaceMethodName(node, "parseLong");
                }
                if (isMethod(node, "java.lang.Boolean", "valueOf", "java.lang.String")) {
                    return replaceMethodName(node, "parseBoolean");
                }
                if (is(node, "java.lang.Float")) {
                    return replaceMethodName(node, "parseFloat");
                }
                if (is(node, "java.lang.Double")) {
                    return replaceMethodName(node, "parseDouble");
                }
            }
        }

        final ITypeBinding typeBinding = node.getExpression().resolveTypeBinding();
        if (typeBinding != null
                && node.getExpression() instanceof ClassInstanceCreation) {
            final List<Expression> cicArgs = arguments((ClassInstanceCreation) node.getExpression());
            if (cicArgs.size() == 1) {
                final Expression arg0 = cicArgs.get(0);
                if (arguments(node).isEmpty() && hasType(arg0, "java.lang.String")) {
                    final String methodName = getMethodName(
                            typeBinding.getQualifiedName(), node.getName().getIdentifier());
                    if (methodName != null) {
                        ctx.getRefactorings().replace(
                                node,
                                newMethodInvocation(typeBinding.getName(), methodName, arg0));
                        return DO_NOT_VISIT_SUBTREE;
                    }
                }
            }
        }
        return VISIT_SUBTREE;
    }

    private boolean is(MethodInvocation node, String declaringTypeQualifiedName) {
        return isMethod(node, declaringTypeQualifiedName, "valueOf", "java.lang.String")
                || (isMethod(node, declaringTypeQualifiedName, "valueOf", "java.lang.String", "int")
                        && equal(10, arguments(node).get(1).resolveConstantExpressionValue()));
    }

    private boolean replaceMethodName(MethodInvocation node, String methodName) {
        final SimpleName name = this.ctx.getASTBuilder().simpleName(methodName);
        this.ctx.getRefactorings().set(node, MethodInvocation.NAME_PROPERTY, name);
        return DO_NOT_VISIT_SUBTREE;
    }

    private boolean replaceWithTheSingleArgument(MethodInvocation node) {
        final ASTBuilder b = this.ctx.getASTBuilder();
        this.ctx.getRefactorings().replace(node, b.copy(arg0(node)));
        return DO_NOT_VISIT_SUBTREE;
    }

    private String getMethodName(final String typeName,
            final String invokedMethodName) {
        if ("java.lang.Boolean".equals(typeName)
                && "booleanValue".equals(invokedMethodName)) {
            return "valueOf";
        } else if ("java.lang.Byte".equals(typeName)
                && "byteValue".equals(invokedMethodName)) {
            return "parseByte";
        } else if ("java.lang.Double".equals(typeName)
                && "doubleValue".equals(invokedMethodName)) {
            return "parseDouble";
        } else if ("java.lang.Float".equals(typeName)
                && "floatValue".equals(invokedMethodName)) {
            return "parseFloat";
        } else if ("java.lang.Long".equals(typeName)
                && "longValue".equals(invokedMethodName)) {
            return "parseLong";
        } else if ("java.lang.Integer".equals(typeName)
                && "intValue".equals(invokedMethodName)) {
            return "parseInt";
        }
        return null;
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        final ITypeBinding typeBinding = node.getType().resolveBinding();
        final List<Expression> args = arguments(node);
        if (getJavaMinorVersion() >= 5 && args.size() == 1) {
            if (hasType(typeBinding,
                    "java.lang.Boolean",
                    "java.lang.Byte",
                    "java.lang.Character",
                    "java.lang.Double",
                    "java.lang.Long",
                    "java.lang.Short",
                    "java.lang.Integer")) {
                replaceWithValueOf(node, typeBinding);
                return DO_NOT_VISIT_SUBTREE;
            } else if (hasType(typeBinding, "java.lang.Float")) {
                return replaceFloatInstanceWithValueOf(node, typeBinding, args);
            }
        }
        return VISIT_SUBTREE;
    }

    private boolean replaceFloatInstanceWithValueOf(ClassInstanceCreation node, final ITypeBinding typeBinding,
            final List<Expression> args) {
        final Expression arg0 = args.get(0);
        if (isPrimitive(arg0, "double")) {
            final ASTBuilder b = ctx.getASTBuilder();
            ctx.getRefactorings().replace(
                    node,
                    b.invoke(typeBinding.getName(), "valueOf", b.cast("float", b.copy(arg0))));
            return DO_NOT_VISIT_SUBTREE;
        } else if (hasType(arg0, "java.lang.Double")) {
            final ASTBuilder b = ctx.getASTBuilder();
            ctx.getRefactorings().replace(
                    node,
                    b.invoke(b.copy(arg0), "floatValue"));
            return DO_NOT_VISIT_SUBTREE;
        } else {
            replaceWithValueOf(node, typeBinding);
            return DO_NOT_VISIT_SUBTREE;
        }
    }

    private void replaceWithValueOf(ClassInstanceCreation node,
            final ITypeBinding typeBinding) {
        this.ctx.getRefactorings().replace(
                node,
                newMethodInvocation(typeBinding.getName(), "valueOf", arguments(node).get(0)));
    }

    private MethodInvocation newMethodInvocation(String typeName,
            String methodName, Expression arg) {
        final ASTBuilder b = this.ctx.getASTBuilder();
        return b.invoke(typeName, methodName, b.copy(arg));
    }

    @Override
    public boolean visit(QualifiedName node) {
        final ASTNode parent = removeParentheses(node.getParent());
        if (parent instanceof VariableDeclarationFragment) {
            final ITypeBinding typeBinding = resolveTypeBinding((VariableDeclarationFragment) parent);
            return replaceBooleanObjectByPrimitive(node, typeBinding);
        } else if (parent instanceof Assignment) {
            final ITypeBinding typeBinding = ((Assignment) parent).resolveTypeBinding();
            return replaceBooleanObjectByPrimitive(node, typeBinding);
        }
        return VISIT_SUBTREE;
    }

    private boolean replaceBooleanObjectByPrimitive(QualifiedName node, ITypeBinding typeBinding) {
        if (typeBinding != null && typeBinding.isPrimitive()) {
            if (isField(node, Boolean.class.getName(), "TRUE")) {
                return replaceWithBooleanLiteral(node, true);
            } else if (isField(node, Boolean.class.getName(), "FALSE")) {
                return replaceWithBooleanLiteral(node, false);
            }
        }
        return VISIT_SUBTREE;
    }

    private boolean replaceWithBooleanLiteral(QualifiedName node, boolean val) {
        final BooleanLiteral booleanLiteral = this.ctx.getASTBuilder().boolean0(val);
        this.ctx.getRefactorings().replace(node, booleanLiteral);
        return DO_NOT_VISIT_SUBTREE;
    }
}
