package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.Name

/**
 * Java 互操作类型传播约束检查器
 *
 * 对齐 C++ DiagsInterop.cpp:
 * - `VARIABLE_OF_JAVA_TYPE` (sema_variable_of_java_type): 变量/字段的类型是 Java 互操作类型,
 *   但其声明上下文不是 Java 互操作声明(即外层 class/struct/interface 没有 @Java/@JavaMirror/@JavaImpl)。
 * - `GENERIC_PARAMETER_OF_JAVA_TYPE` (sema_generic_parameter_of_java_type): 用 Java 互操作类型
 *   作为普通(非 Java 互操作)泛型声明的类型实参。
 *
 * 注册为 callableDeclarationCheckers。
 */
object CfirJavaInteropTypePropagationChecker : CfirCallableDeclarationChecker() {
    /**
     * Java 互操作基础注解名。
     */
    private val JAVA = Name.identifier("Java")

    /**
     * Java mirror 互操作注解名。
     */
    private val JAVA_MIRROR = Name.identifier("JavaMirror")

    /**
     * Java implementation 互操作注解名。
     */
    private val JAVA_IMPL = Name.identifier("JavaImpl")

    /**
     * 所有可使声明进入 Java 互操作语义的注解名集合。
     */
    private val JAVA_ANN_NAMES = setOf(JAVA, JAVA_MIRROR, JAVA_IMPL)

    /**
     * 检查字段或属性类型是否把 Java 互操作类型传播到非 Java 互操作上下文。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirCallableDeclaration) {
        val (declKind, varType) = when (declaration) {
            is CfirFieldVariable -> "field" to (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            is CfirProperty -> "property" to (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            else -> return
        }
        val type = varType ?: return

        val enclosingJava = enclosingClassIsJavaInterop(declaration)

        if (!enclosingJava) {
            val primaryJava = context.firstJavaInteropClass(type)
            if (primaryJava != null) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.VARIABLE_OF_JAVA_TYPE,
                    a = declKind,
                    b = type,
                )
            }
        }

        // GENERIC_PARAMETER_OF_JAVA_TYPE:类型实参若为 Java 互操作类型,
        // 但被实例化的泛型主类不是 Java 互操作类型。
        checkGenericJavaTypeArgument(declaration, type)
    }

    /**
     * 检查普通泛型主类型的类型实参中是否包含 Java 互操作类型。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkGenericJavaTypeArgument(declaration: CfirCallableDeclaration, type: ConeCangJieType) {
        val classLike = type as? ConeClassLikeType ?: return
        if (classLike.typeArguments.isEmpty()) return
        val mainDecl = context.session.symbolProvider
            .getClassLikeSymbolByClassId(classLike.classId)?.cfir
            as? CfirClassLikeDeclaration ?: return
        if (mainDecl.hasAnyJavaInteropAnnotation()) return

        for (arg in classLike.typeArguments) {
            val argType = arg.type ?: continue
            val javaArg = context.firstJavaInteropClass(argType) ?: continue
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.GENERIC_PARAMETER_OF_JAVA_TYPE,
                a = mainDecl.name,
                b = javaArg,
            )
        }
    }

    /**
     * 在类型及其类型实参中查找第一个 Java 互操作 class-like 类型。
     */
    private fun CheckerContext.firstJavaInteropClass(type: ConeCangJieType): ConeCangJieType? {
        if (type is ConeClassLikeType) {
            val decl = session.symbolProvider
                .getClassLikeSymbolByClassId(type.classId)?.cfir
                as? CfirClassLikeDeclaration
            if (decl != null) {
                if (decl.hasAnyJavaInteropAnnotation()) {
                    return type
                }
            }
            for (arg in type.typeArguments) {
                val inner = arg.type ?: continue
                firstJavaInteropClass(inner)?.let { return it }
            }
        }
        return null
    }

    /**
     * 判断 callable 所属 class-like 声明是否为 Java 互操作声明。
     */
    context(context: CheckerContext)
    private fun enclosingClassIsJavaInterop(declaration: CfirCallableDeclaration): Boolean {
        val symbol = declaration.symbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>
        val classId = symbol?.callableId?.classId ?: return false
        val ownerDecl = context.session.symbolProvider
            .getClassLikeSymbolByClassId(classId)?.cfir ?: return false
        return ownerDecl.hasAnyJavaInteropAnnotation()
    }

    /**
     * 判断 class-like 声明是否带任一 Java 互操作注解。
     */
    private fun CfirClassLikeDeclaration.hasAnyJavaInteropAnnotation(): Boolean =
        JAVA_ANN_NAMES.any(::hasAnnotation)
}
