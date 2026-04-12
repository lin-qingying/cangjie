package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name

/**
 * 通用语义检查器（General 分组）
 *
 * 对齐 C++ sema_conflict_with_sub_package 等通用语义诊断。
 *
 * 当前实现范围：
 * - 子包冲突检查将在 CfirCangJieScopeProvider 增加 getSubPackageNames 后启用
 *
 * TODO: 待 CfirCangJieScopeProvider 扩展后实现 CONFLICT_WITH_SUB_PACKAGE 检查
 */
object CfirGeneralSemanticsChecker : CfirFileChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        // 子包冲突检查暂缓，等待 CfirCangJieScopeProvider.getSubPackageNames API
    }
}

/**
 * 类/结构体/枚举语义检查器（ClassStruct 分组）
 *
 * 对齐 C++ sema_non_abstract_class_cannot_be_sealed、sema_type_uninitialized_static_field 等。
 */
object CfirClassStructSemanticsChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration is CfirClass) {
            checkSealedOnlyOnAbstract(declaration)
            checkStaticVariableGenericParameterDependency(declaration)
        }
        if (declaration is CfirStruct || declaration is CfirEnum) {
            checkStaticVariableGenericParameterDependency(declaration)
        }
    }

    /**
     * 非抽象类不能使用 sealed 修饰。
     *
     * 对齐 C++ DiagKind::sema_non_abstract_class_cannot_be_sealed
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSealedOnlyOnAbstract(classDecl: CfirClass) {
        if (!classDecl.status.isSealed) return
        if (classDecl.status.isAbstract) return

        reporter.reportOn(
            source = classDecl.source,
            factory = CfirErrors.NON_ABSTRACT_CLASS_CANNOT_BE_SEALED,
        )
    }

    /**
     * static 变量的类型不能依赖所在类的泛型参数。
     *
     * 对齐 C++ TypeChecker::CheckStaticVarAccessNonStatic
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkStaticVariableGenericParameterDependency(classLike: CfirClassLikeDeclaration) {
        val typeParameterNames = when (classLike) {
            is CfirClass -> classLike.typeParameters.map { it.name }.toSet()
            is CfirStruct -> classLike.typeParameters.map { it.name }.toSet()
            is CfirEnum -> classLike.typeParameters.map { it.name }.toSet()
            is CfirInterface -> classLike.typeParameters.map { it.name }.toSet()
            else -> return
        }
        if (typeParameterNames.isEmpty()) return

        for (member in classLike.declarations) {
            val fieldVariable = member as? CfirFieldVariable ?: continue
            if (!fieldVariable.status.isStatic) continue
            val resolvedType = (fieldVariable.returnTypeRef as? CfirResolvedTypeRef)
                ?.coneType ?: continue
            for (typeParamName in typeParameterNames) {
                if (resolvedType.containsTypeParameter(typeParamName)) {
                    reporter.reportOn(
                        source = fieldVariable.source,
                        factory = CfirErrors.STATIC_VARIABLE_USE_GENERIC_PARAMETER,
                        a = typeParamName,
                    )
                    break
                }
            }
        }
    }
}

/**
 * 属性语义检查器（Property 分组）
 *
 * 对齐 C++ sema_property_must_have_accessors 等。
 */
object CfirPropertySemanticsChecker : CfirPropertyChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirProperty) {
        checkPropertyAccessors(declaration)
        checkImmutablePropertySetter(declaration)
    }

    /**
     * 属性必须有访问器（getter 或 setter）。
     *
     * 对齐 C++ DiagKind::sema_property_must_have_accessors
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPropertyAccessors(property: CfirProperty) {
        if (property.getter == null && property.setter == null) {
            reporter.reportOn(
                source = property.source,
                factory = CfirErrors.PROPERTY_MUST_HAVE_ACCESSORS,
            )
        }
    }

    /**
     * 不可变属性不能有 setter。
     *
     * 对齐 C++ DiagKind::sema_immutable_property_with_setter
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkImmutablePropertySetter(property: CfirProperty) {
        if ( property.setter != null) {
            reporter.reportOn(
                source = property.source,
                factory = CfirErrors.IMMUTABLE_PROPERTY_WITH_SETTER,
            )
        }
    }
}

// 工具函数：从声明中提取名称
private fun CfirDeclaration.declarationName(): Name? = when (this) {
    is CfirClass -> name
    is CfirInterface -> name
    is CfirStruct -> name
    is CfirEnum -> name
    is CfirNamedFunction -> name
    is CfirProperty -> name
    is CfirFieldVariable -> name
    is CfirTypeAlias -> name
    else -> null
}

// 工具函数：检查 ConeCangJieType 是否包含指定名称的类型参数
private fun ConeCangJieType.containsTypeParameter(name: Name): Boolean {
    if (this is ConeTypeParameterType && this.lookupTag.name == name) return true
    for (arg in this.typeArguments) {
        val argType = arg.type ?: continue
        if (argType.containsTypeParameter(name)) return true
    }
    return false
}
