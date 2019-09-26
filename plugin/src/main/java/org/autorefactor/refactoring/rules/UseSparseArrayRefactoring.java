package org.autorefactor.refactoring.rules;

import static org.autorefactor.refactoring.ASTHelper.DO_NOT_VISIT_SUBTREE;
import static org.autorefactor.refactoring.ASTHelper.VISIT_SUBTREE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.autorefactor.refactoring.ASTBuilder;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WildcardType;

@SuppressWarnings("unchecked")
// We are extending the method from abstract refactoring rule to usesparse rule.
public class UseSparseArrayRefactoring extends AbstractRefactoringRule {

	CompilationUnit cu = null;
	boolean importFlag = true;

	private static boolean USE_SIMPLE_TYPE_NAME = true;
	static Map<String, String> subsClassMap;
	// static is some value which stored in memory when we call the costructor in the class..
	static {
		subsClassMap = new HashMap<String, String>();
		subsClassMap.put("java.util.HashMap<java.lang.Integer,java.lang.Object>", "SparseArray");
		subsClassMap.put("java.util.HashMap<java.lang.Integer,java.lang.Integer>", "SparseIntArray");
		subsClassMap.put("java.util.HashMap<java.lang.Integer,java.lang.Boolean>", "SparseBooleanArray");
		subsClassMap.put("java.util.HashMap<java.lang.Integer,java.lang.Long>", "SparseLongArray");
		subsClassMap.put("java.util.HashMap<java.lang.Long,java.lang.Object>", "LongSparseArray");
		subsClassMap.put("java.util.HashMap<java.lang.Long,java.lang.Long>", "LongSparseLongArray");
	}

	@Override
	public String getDescription() {
		return "Replace HashMap with SparseArray";
	}

	@Override
	public String getName() {
		return "UseSparseArrayRefactoring";
	}

	@Override
	public boolean visit(CompilationUnit node) {
		cu = node;
		return VISIT_SUBTREE;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
	 * VariableDeclarationStatement)
	 * 
	 * This method finds variable declaration fragment where HashMap<type, type> is
	 * declared and applies refactoring.
	 */
	@Override
	//ariableDeclarationFragment is used to get the L.H.S declaration of any statement..e.g int x =new int();
	public boolean visit(VariableDeclarationFragment fragment) {
		// to get extra information regarding fragemente use reslove binding
		IVariableBinding varBinding = fragment.resolveBinding();
		if (varBinding != null) {
			ITypeBinding typeBinding = varBinding.getType();
			String typeName = typeBinding.getQualifiedName();
			String typeClass = typeBinding.getTypeDeclaration().getQualifiedName();
			if (typeClass.equals("java.util.Map"))
				typeClass = "java.util.HashMap";
			if (subsClassMap.containsKey(typeName)) {
				refactorType(fragment, typeName);
				return DO_NOT_VISIT_SUBTREE;
			} else if (typeClass.equals("java.util.HashMap")) {
				ITypeBinding[] typeArgs = typeBinding.getTypeArguments();
				if (typeArgs.length == 2) {
					String firstTypeArgQName = typeArgs[0].getQualifiedName();
					if (firstTypeArgQName.equals("java.lang.Integer") || firstTypeArgQName.equals("java.lang.Long")) {
						typeName = typeClass + "<" + firstTypeArgQName + ",java.lang.Object>";
						refactorParameterizedType(fragment, typeName, typeArgs[1]);
					}
				}
				return DO_NOT_VISIT_SUBTREE;
			}
		}
		return VISIT_SUBTREE;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
	 * ClassInstanceCreation)
	 * 
	 * finds class instantiations where HashMap<type, type> is instantiated and
	 * applies refactoring.
	 */
	@Override
	public boolean visit(ClassInstanceCreation node) {
		ITypeBinding typeBinding = node.resolveTypeBinding();
		if (typeBinding != null) {
			String typeName = typeBinding.getQualifiedName();
			if (subsClassMap.containsKey(typeName)) {
				refactorType(node, typeName);
				return DO_NOT_VISIT_SUBTREE;
			} else if (typeBinding.getTypeDeclaration().getQualifiedName().equals("java.util.HashMap")) {
				ITypeBinding[] typeArgs = typeBinding.getTypeArguments();
				if (typeArgs.length == 2) {
					String firstTypeArgQName = typeArgs[0].getQualifiedName();
					if (firstTypeArgQName.equals("java.lang.Integer") || firstTypeArgQName.equals("java.lang.Long")) {
						typeName = "java.util.HashMap<" + firstTypeArgQName + ",java.lang.Object>";
						refactorParameterizedType(node, typeName, typeArgs[1]);
					}
				}
				return DO_NOT_VISIT_SUBTREE;
			}
		}
		return VISIT_SUBTREE;
	}

	/**
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.
	 * MethodDeclaration)
	 * 
	 * finds method declarations where return type is HashMap and applies
	 * refactoring.
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		if (node.getReturnType2() != null) {
			ITypeBinding typeBinding = node.getReturnType2().resolveBinding();
			IMethodBinding methodBinding = node.resolveBinding();
			if (methodBinding != null) {
				String returnType = methodBinding.getReturnType().getQualifiedName();
				String returnTypeClass = typeBinding.getTypeDeclaration().getQualifiedName();
				if (returnTypeClass.equals("java.util.Map"))
					returnTypeClass = "java.util.HashMap";
				if (subsClassMap.containsKey(returnType)) {
					refactorReturnType(node, returnType);
					return DO_NOT_VISIT_SUBTREE;
				} else if (returnTypeClass.equals("java.util.HashMap")) {
					ITypeBinding[] typeArgs = typeBinding.getTypeArguments();
					if (typeArgs.length == 2) {
						String firstTypeArgQName = typeArgs[0].getQualifiedName();
						if (firstTypeArgQName.equals("java.lang.Integer")
								|| firstTypeArgQName.equals("java.lang.Long")) {
							returnType = "java.util.HashMap<" + firstTypeArgQName + ",java.lang.Object>";
							refactorParameterizedType(node, returnType, typeArgs[1]);
						}
					}
					return DO_NOT_VISIT_SUBTREE;
				}
			}
		}
		return VISIT_SUBTREE;
	}

	/**
	 * replaces variable declaration of HashMap type using
	 * parameterized SparseArray or LongSparseArray
	 * 
	 * @param fragment
	 * @param typeName
	 * @param typeArg
	 */
	private void refactorParameterizedType(VariableDeclarationFragment fragment, String typeName,
			ITypeBinding typeArg) {
		final AST ast = fragment.getAST();
		final ASTBuilder b = this.ctx.getASTBuilder();
		final Type type = b.type(subsClassMap.get(typeName));
		final ParameterizedType parameterizedType = ast.newParameterizedType(type);
		// Type genericType = ast.newSimpleType(ast.newSimpleName(typeArg.getName()));
		Type genericType = typeFromBinding(ast, typeArg);
		parameterizedType.typeArguments().add(genericType);
		ASTNode parent = fragment.getParent();
		addImport();
		if (parent instanceof FieldDeclaration)
			ctx.getRefactorings().replace(((FieldDeclaration) parent).getType(), parameterizedType);
		else if (parent instanceof VariableDeclarationStatement)
			ctx.getRefactorings().replace(((VariableDeclarationStatement) parent).getType(), parameterizedType);
	}

	/**
	 * replaces variable declaration of HashMap type using type
	 * defined in {@link #subsClassMap}
	 * 
	 * @param fragment
	 * @param typeName
	 */
	private void refactorType(final VariableDeclarationFragment fragment, String typeName) {
		final ASTBuilder b = this.ctx.getASTBuilder();
		final Type primitiveType = b.type(subsClassMap.get(typeName));
		ASTNode parent = fragment.getParent();
		addImport();
		if (parent instanceof FieldDeclaration)
			ctx.getRefactorings().replace(((FieldDeclaration) parent).getType(), primitiveType);
		else if (parent instanceof VariableDeclarationStatement)
			ctx.getRefactorings().replace(((VariableDeclarationStatement) parent).getType(), primitiveType);
	}

	/**
	 * replaces class instantiation of HashMap type using
	 * parameterized SparseArray or LongSparseArray
	 * 
	 * @param node
	 * @param typeName
	 * @param typeArg
	 */
	private void refactorParameterizedType(ClassInstanceCreation node, String typeName, ITypeBinding typeArg) {
		final AST ast = node.getAST();
		final ASTBuilder b = this.ctx.getASTBuilder();
		final Type type = b.type(subsClassMap.get(typeName));
		final ParameterizedType parameterizedType = ast.newParameterizedType(type);
		Type genericType = typeFromBinding(ast, typeArg);
		parameterizedType.typeArguments().add(genericType);
		addImport();
		ctx.getRefactorings().replace(node.getType(), parameterizedType);
	}

	/**
	 * replaces class instantiation of HashMap type using type
	 * defined in {@link #subsClassMap}
	 * 
	 * @param node
	 * @param typeName
	 */
	private void refactorType(final ClassInstanceCreation node, String typeName) {
		final ASTBuilder b = this.ctx.getASTBuilder();
		final Type primitiveType = b.type(subsClassMap.get(typeName));
		addImport();
		ctx.getRefactorings().replace(node.getType(), primitiveType);
	}

	/**
	 * replaces method declaration of HashMap type using
	 * parameterized SparseArray or LongSparseArray
	 * 
	 * @param node
	 * @param returnType
	 * @param typeArg
	 */
	private void refactorParameterizedType(MethodDeclaration node, String returnType, ITypeBinding typeArg) {
		final AST ast = node.getAST();
		final ASTBuilder b = this.ctx.getASTBuilder();
		final Type type = b.type(subsClassMap.get(returnType));
		final ParameterizedType parameterizedType = ast.newParameterizedType(type);
		// Type genericType = ast.newSimpleType(ast.newSimpleName(typeArg.getName()));
		Type genericType = typeFromBinding(ast, typeArg);
		parameterizedType.typeArguments().add(genericType);
		addImport();
		ctx.getRefactorings().replace(node.getReturnType2(), parameterizedType);
	}

	/**
	 * replaces method declaration of HashMap type using
	 * type defined in {@link #subsClassMap}
	 * 
	 * @param node
	 * @param returnTypeString
	 */
	private void refactorReturnType(MethodDeclaration node, String returnTypeString) {
		final ASTBuilder b = this.ctx.getASTBuilder();
		final Type returnType = b.type(subsClassMap.get(returnTypeString));
		addImport();
		ctx.getRefactorings().replace(node.getReturnType2(), returnType);
	}

	/**
	 * converts ITypeBinding object to Type object
	 * 
	 * @param ast
	 * @param typeBinding
	 * @return type
	 */
	public static Type typeFromBinding(AST ast, ITypeBinding typeBinding) {
		if (ast == null)
			throw new NullPointerException("ast is null");
		if (typeBinding == null)
			throw new NullPointerException("typeBinding is null");

		if (typeBinding.isPrimitive()) {
			return ast.newPrimitiveType(PrimitiveType.toCode(typeBinding.getName()));
		}

		if (typeBinding.isCapture()) {
			ITypeBinding wildCard = typeBinding.getWildcard();
			WildcardType capType = ast.newWildcardType();
			ITypeBinding bound = wildCard.getBound();
			if (bound != null) {
				capType.setBound(typeFromBinding(ast, wildCard.getBound()), wildCard.isUpperbound());
			}
			return capType;
		}

		if (typeBinding.isArray()) {
			Type elType = typeFromBinding(ast, typeBinding.getElementType());
			return ast.newArrayType(elType, typeBinding.getDimensions());
		}

		if (typeBinding.isParameterizedType()) {
			ParameterizedType type = ast.newParameterizedType(typeFromBinding(ast, typeBinding.getErasure()));

			List<Type> newTypeArgs = type.typeArguments();
			for (ITypeBinding typeArg : typeBinding.getTypeArguments()) {
				newTypeArgs.add(typeFromBinding(ast, typeArg));
			}

			return type;
		}

		// simple or raw type
		if (USE_SIMPLE_TYPE_NAME) {
			String simpleName = typeBinding.getName();
			if ("".equals(simpleName)) {
				throw new IllegalArgumentException("No name for type binding.");
			}
			return ast.newSimpleType(ast.newName(simpleName));
		} else {
			String qualName = typeBinding.getQualifiedName();
			if ("".equals(qualName)) {
				throw new IllegalArgumentException("No name for type binding.");
			}
			return ast.newSimpleType(ast.newName(qualName));
		}
	}

	/**
	 * adds import android.util.*
	 */
	private void addImport() {
		List<ImportDeclaration> importList = cu.imports();
		AST ast = cu.getAST();
		ImportDeclaration id = ast.newImportDeclaration();
		id.setName(ast.newName("android.util"));
		id.setOnDemand(true);
		for (ImportDeclaration importdec : importList) {
			String imp = importdec.toString().trim();
			if (imp.equalsIgnoreCase("import android.util.*;"))
				importFlag = false;
		}
		if (importList.size() > 0 && importFlag) {
			ctx.getRefactorings().insertAfter(id, importList.get(0));
			importFlag = false;
		}
	}
}
