package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * ThreadContext 继承约束检查器。
 *
 * 对齐官方 C++ TypeCheckerImpl::CheckThreadContextInheritance
 * （`external/cangjie_compiler/src/Sema/TypeCheckClassLike.cpp:74-95`）：
 * - 任何用户声明直接以 `<: ThreadContext` 继承/实现（class 或 interface）都不合法，
 *   除非该声明被 `open` 修饰（此时报 `INHERIT_THREAD_CONTEXT_NOT_OPEN`），或属于
 *   白名单（仅 std.core.ThreadContext / ohos.base.MainThreadContext）。
 * - 通过 `extend X <: ThreadContext` 引入继承时，同样对目标声明 X 施加同一约束。
 */
object CfirInheritanceThreadContextChecker : CfirClassLikeChecker() {
    /**
     * `ThreadContext` 类型的短名。
     */
    internal val THREAD_CONTEXT: Name = Name.identifier("ThreadContext")

    /**
     * 检查 class / interface 的直接父类型中是否包含 `ThreadContext`。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        // struct 不能继承类；只有普通 class 与 interface 可能 `<: ThreadContext`。
        if (declaration !is CfirClass && declaration !is CfirInterface) return
        if (!declaration.directlyInheritsThreadContext()) return
        checkThreadContextInheritance(declaration, declaration.source, reporter)
    }
}

/**
 * `extend X <: ThreadContext` 的 ThreadContext 继承约束检查器。
 *
 * 官方在 TypeCheckExtend.cpp:489 对 extendedDecl 复用同一检查，诊断锚在被扩展的目标
 * 声明源码上（本仓库仅报告 INHERIT_THREAD_CONTEXT_INVALID，白名单/not-open 不影响夹具）。
 */
object CfirExtendThreadContextChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        val hasThreadContextSuper = extend.superTypeRefs.any { superTypeRef ->
            superTypeRef.toResolvedThreadContextClassId() == CfirInheritanceThreadContextChecker.THREAD_CONTEXT
        }
        if (!hasThreadContextSuper) return
        val target = CfirExtendSemantics.targetDeclaration(context, extend) ?: return
        checkThreadContextInheritance(target, target.source, reporter)
    }
}

/**
 * 应用官方 CheckThreadContextInheritance 的判定。
 */
private context(diagnosticContext: DiagnosticContext)
fun checkThreadContextInheritance(
    declaration: CfirClassLikeDeclaration,
    source: CjSourceElement?,
    reporter: DiagnosticReporter,
) {
    // 1) 继承 ThreadContext 的声明带 `open` → NOT_OPEN（官方先判 open 并直接返回）。
    if (declaration.status.isOpen) {
        reporter.reportOn(source, CfirErrors.INHERIT_THREAD_CONTEXT_NOT_OPEN, declaration.name)
        return
    }
    // 2) 白名单（std.core.ThreadContext / ohos.base.MainThreadContext）放行。
    if (declaration.name.asString() == "ThreadContext" || declaration.name.asString() == "MainThreadContext") {
        return
    }
    // 3) 其余用户声明 → INVALID。
    reporter.reportOn(source, CfirErrors.INHERIT_THREAD_CONTEXT_INVALID, declaration.name)
}

/**
 * 判断 class-like 声明的直接父类型中是否包含 `ThreadContext`。
 */
private fun CfirClassLikeDeclaration.directlyInheritsThreadContext(): Boolean =
    superTypeRefs.any { superTypeRef ->
        superTypeRef.toResolvedThreadContextClassId() == CfirInheritanceThreadContextChecker.THREAD_CONTEXT
    }

/**
 * 将已解析的 super type ref 解析为 `ThreadContext` 短名（仅当它确实是 ThreadContext 时返回）。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toResolvedThreadContextClassId(): Name? =
    (this as? CfirResolvedTypeRef)?.coneType
        ?.takeIf { it !is ConeErrorType }
        ?.let { (it as? ConeClassLikeType)?.classId?.shortClassName }