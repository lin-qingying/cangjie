package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.argumentTextAt
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.findAnnotations
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.CfirApiLevelProvider
import org.cangnova.cangjie.cfir.session.apiLevelProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.Name

/**
 * APILevel 引用检查器。
 *
 * 对齐 C++:
 * - sema_apilevel_ref_higher (PluginCustomAnnoChecker.cpp): 调用 `@APILevel(N)` 声明
 *   时当前项目 APILevel < N 报错。
 * - sema_apilevel_syscap_error / _warning (PluginCustomAnnoChecker.cpp:555/566):
 *   调用 `@Syscap(s)` 声明时 s ∉ union 报 error, s ∉ intersection 报 warning。
 *
 * 通过 [CfirApiLevelProvider] 读项目级配置;未启用时跳过,避免非 ohos 场景误报。
 */
object CfirApiLevelRefHigherChecker : CfirFunctionCallChecker() {
    /**
     * OHOS API 级别注解的短名。
     *
     * 该名字用于在被调用声明的注解列表中定位 `@APILevel`，并读取第一个实参作为
     * 声明可用的最低项目 API 级别。
     */
    private val API_LEVEL = Name.identifier("APILevel")

    /**
     * OHOS Syscap 注解的短名。
     *
     * 该名字用于定位 `@Syscap`，再结合项目级 union/intersection 配置区分错误与警告。
     */
    private val SYSCAP = Name.identifier("Syscap")

    /**
     * 检查函数调用目标上的 APILevel 与 Syscap 约束。
     *
     * 当当前会话未启用 API 级别或 Syscap 检查时直接跳过；启用后从解析出的 callable
     * 目标读取注解实参，并把项目配置与声明要求转换成对应诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val provider = context.session.apiLevelProvider
        val projectLevel = provider.projectApiLevel
        val syscapActive = provider.syscapEnabled

        if (projectLevel == CfirApiLevelProvider.DISABLED && !syscapActive) return

        val ref = expression.calleeReference as? CfirResolvedNamedReference ?: return
        val target = (ref.resolvedSymbol as? CfirCallableSymbol<*>)?.cfir ?: return

        if (projectLevel != CfirApiLevelProvider.DISABLED) {
            val targetLevel = extractApiLevel(target)
            if (targetLevel != null && targetLevel > projectLevel) {
                reporter.reportOn(
                    source = expression.source,
                    factory = CfirErrors.APILEVEL_REF_HIGHER,
                    a = nameOf(target),
                    b = targetLevel,
                    c = projectLevel,
                )
            }
        }

        if (syscapActive) {
            val targetSyscap = extractSyscap(target)
            if (targetSyscap != null) {
                val nameArg = Name.identifier(targetSyscap)
                if (targetSyscap !in provider.syscapUnion) {
                    reporter.reportOn(
                        source = expression.source,
                        factory = CfirErrors.APILEVEL_SYSCAP_ERROR,
                        a = nameArg,
                    )
                } else if (targetSyscap !in provider.syscapIntersection) {
                    reporter.reportOn(
                        source = expression.source,
                        factory = CfirErrors.APILEVEL_SYSCAP_WARNING,
                        a = nameArg,
                    )
                }
            }
        }
    }

    /**
     * 取得诊断中用于展示的被调用声明名称。
     *
     * 函数、属性与类状声明分别使用自身名称或短类名，未知声明保留错误占位名，避免
     * 诊断构造阶段依赖具体 CFIR 子类。
     */
    private fun nameOf(decl: CfirDeclaration): Name = when (decl) {
        is CfirNamedFunction -> decl.name
        is CfirProperty -> decl.name
        is CfirClassLikeDeclaration -> decl.symbol.classId.shortClassName
        else -> Name.identifier("<unknown>")
    }

    /**
     * 读取 callable 声明上的 `@APILevel` 整数实参。
     *
     * 注解缺失或实参不是整数文本时返回 `null`，由调用方决定是否需要继续报告。
     */
    private fun extractApiLevel(decl: CfirCallableDeclaration): Int? {
        val entry = findAnnotation(decl, API_LEVEL) ?: return null
        return entry.argumentTextAt(0)?.toIntOrNull()
    }

    /**
     * 读取 callable 声明上的 `@Syscap` 文本实参。
     *
     * 返回值保持注解实参原始文本，后续会转换为 [Name] 并与项目 syscap 集合比较。
     */
    private fun extractSyscap(decl: CfirCallableDeclaration): String? {
        val entry = findAnnotation(decl, SYSCAP) ?: return null
        return entry.argumentTextAt(0)
    }

    /**
     * 在 callable 声明注解列表中查找指定短名的第一个注解调用。
     *
     * 这里复用声明检查器的注解查询工具，避免表达式检查器直接理解注解存储细节。
     */
    private fun findAnnotation(decl: CfirCallableDeclaration, annName: Name): CfirAnnotationCall? =
        decl.findAnnotations(annName).firstOrNull() as? CfirAnnotationCall
}
