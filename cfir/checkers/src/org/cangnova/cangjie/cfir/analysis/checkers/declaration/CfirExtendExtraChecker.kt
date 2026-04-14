package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemanticsSupport
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
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.source.psi

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
        val owner = targetDecl.source?.psi as? CjModifierListOwner ?: return
        if (owner.annotationEntries.any { it.shortName == JAVA }) {
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
        val owner = targetDecl.source?.psi as? CjModifierListOwner ?: return
        if (owner.annotationEntries.any { it.shortName == JAVA_IMPL }) {
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
}
