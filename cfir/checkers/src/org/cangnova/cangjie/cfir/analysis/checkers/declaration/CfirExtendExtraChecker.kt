package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.Name

/**
 * Extend 补充检查器（ExtendExtra 分组）
 *
 * 对齐 C++ TypeCheckExtend.cpp 中未被已有 9 个 Extend checker 覆盖的诊断：
 * - EXTEND_FUNCTION_CANNOT_OVERRIDDEN: extend 中的函数不能被 override
 * - EXTEND_MEMBER_CANNOT_SHADOW: extend 成员不能遮蔽原有成员
 * - EXTEND_ILLEGAL_MEMBER: extend 中不允许的成员类型（如构造器、字段）
 * - EXTEND_A_JAVA_TYPE: 不能 extend @Java 标注的类型
 * - EXTEND_REF_TARGET_CANNOT_BE_JAVA_IMPL: extend 不能指向 @JavaImpl 声明
 * - TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE: 不能 extend 导入的接口
 */
object CfirExtendExtraChecker : CfirExtendChecker() {
    private val JAVA = Name.identifier("Java")
    private val JAVA_IMPL = Name.identifier("JavaImpl")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        checkIllegalMembers(extend)
        checkExtendJavaType(extend)
        checkExtendJavaImplTarget(extend)
        checkOverrideInExtend(extend)
        checkMemberShadowing(extend)
        checkExtendImportedInterface(extend)
    }

    /**
     * extend 中不允许构造器、字段变量等成员。
     *
     * 对齐 C++ DiagKind::sema_extend_illegal_member
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkIllegalMembers(extend: CfirExtend) {
        for (member in extend.declarations) {
            when (member) {
                is CfirConstructor -> {
                    reporter.reportOn(
                        source = member.source ?: extend.source,
                        factory = CfirErrors.EXTEND_ILLEGAL_MEMBER,
                    )
                }
                is CfirFieldVariable -> {
                    reporter.reportOn(
                        source = member.source ?: extend.source,
                        factory = CfirErrors.EXTEND_ILLEGAL_MEMBER,
                    )
                }
                else -> Unit
            }
        }
    }

    /**
     * 不能 extend @Java 标注的类型。
     *
     * 对齐 C++ DiagKind::sema_extend_a_java_type
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExtendJavaType(extend: CfirExtend) {
        val targetTypeRef = extend.extendedTypeRef
        val targetType = (targetTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: return
        val targetDecl = context.session.symbolProvider
            .getClassLikeSymbolByClassId(targetType.classId)?.cfir ?: return
        if (CfirExtendSemantics.hasAnnotation(targetDecl, JAVA)) {
            reporter.reportOn(
                source = targetTypeRef.source ?: extend.source,
                factory = CfirErrors.EXTEND_A_JAVA_TYPE,
            )
        }
    }

    /**
     * extend 不能指向 @JavaImpl 声明。
     *
     * 对齐 C++ DiagKind::sema_extend_ref_target_cannot_be_java_impl
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExtendJavaImplTarget(extend: CfirExtend) {
        val targetTypeRef = extend.extendedTypeRef
        val targetType = (targetTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: return
        val targetDecl = context.session.symbolProvider
            .getClassLikeSymbolByClassId(targetType.classId)?.cfir ?: return
        if (CfirExtendSemantics.hasAnnotation(targetDecl, JAVA_IMPL)) {
            reporter.reportOn(
                source = targetTypeRef.source ?: extend.source,
                factory = CfirErrors.EXTEND_REF_TARGET_CANNOT_BE_JAVA_IMPL,
            )
        }
    }

    /**
     * extend 中的函数如果标记了 override，则报错。
     *
     * 对齐 C++ DiagKind::sema_extend_function_cannot_overridden
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOverrideInExtend(extend: CfirExtend) {
        for (member in extend.declarations) {
            if (member is CfirNamedFunction && member.status.isOverride) {
                reporter.reportOn(
                    source = member.source ?: extend.source,
                    factory = CfirErrors.EXTEND_FUNCTION_CANNOT_OVERRIDDEN,
                    a = "function",
                    b = member.name,
                )
            }
        }
    }

    /**
     * extend 成员不能遮蔽被扩展类型的已有成员。
     *
     * 对齐 C++ DiagKind::sema_extend_member_cannot_shadow
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMemberShadowing(extend: CfirExtend) {
        val targetTypeRef = extend.extendedTypeRef
        val targetType = (targetTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: return
        val targetDecl = context.session.symbolProvider
            .getClassLikeSymbolByClassId(targetType.classId)?.cfir as? org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
            ?: return
        val targetScope = context.createUseSiteMemberScope(targetDecl)

        for (member in extend.declarations) {
            val memberName = member.shadowableName() ?: continue
            if (!member.shadowsExistingMember(targetScope)) continue

            val typeName = targetType.classId.shortClassName
            reporter.reportOn(
                source = member.source ?: extend.source,
                factory = CfirErrors.EXTEND_MEMBER_CANNOT_SHADOW,
                a = memberName,
                b = typeName,
            )
        }
    }

    /**
     * extend shadow 必须按成员签名判断，不能只按名称判断。
     *
     * Kotlin FIR 的 extension shadow checker 会比较参数个数、泛型参数个数和 overloadability；
     * 本项目已有 `overrideSignatureKey()` 作为 override/继承共用签名入口，这里复用同一入口，
     * 与官方编译器 `StructInheritanceChecker::CheckExtendMemberValid` 的 member-signature 语义对齐。
     */
    private fun CfirDeclaration.shadowsExistingMember(
        targetScope: org.cangnova.cangjie.cfir.scopes.CfirTypeScope,
    ): Boolean {
        return when (this) {
            is CfirNamedFunction -> {
                val signature = symbol.overrideSignatureKey()
                var found = false
                targetScope.processFunctionsByName(name) { candidate ->
                    if (candidate.isBound && candidate.cfir !== this && candidate.overrideSignatureKey() == signature) {
                        found = true
                    }
                }
                found
            }

            is CfirProperty -> {
                val signature = symbol.overrideSignatureKey()
                var found = false
                targetScope.processPropertiesByName(name) { candidate ->
                    if (candidate.isBound && candidate.cfir !== this && candidate.overrideSignatureKey() == signature) {
                        found = true
                    }
                }
                found
            }

            else -> false
        }
    }

    private fun CfirDeclaration.shadowableName(): Name? = when (this) {
        is CfirNamedFunction -> name
        is CfirProperty -> name
        else -> null
    }

    /**
     * 不能 extend 导入的接口（只能在定义包中 extend）。
     *
     * 对齐 C++ DiagKind::sema_type_cannot_extend_imported_interface
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExtendImportedInterface(extend: CfirExtend) {
        for (superTypeRef in extend.superTypeRefs) {
            val superType = (superTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: continue
            if (CfirExtendSemantics.isProtectedInterface(superType.classId)) continue
            val superDecl = context.session.symbolProvider
                .getClassLikeSymbolByClassId(superType.classId)?.cfir
                as? org.cangnova.cangjie.cfir.declarations.CfirInterface ?: continue

            // 检查接口是否在当前模块中定义
            val interfaceModuleData = superDecl.moduleData
            val extendModuleData = extend.moduleData
            if (interfaceModuleData != extendModuleData) {
                reporter.reportOn(
                    source = superTypeRef.source ?: extend.source,
                    factory = CfirErrors.TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE,
                    a = "extend",
                    b = superType.classId.shortClassName,
                )
            }
        }
    }
}
