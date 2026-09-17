package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.getDeprecation
import org.cangnova.cangjie.resolve.deprecation.DeprecationLevelValue

/**
 * @Deprecated 语义检查器
 *
 * 对齐 C++ DeclAttributeChecker.cpp / TypeCheckReference.cpp `CheckUsageOfDeprecatedWithTarget`
 * 与 `CheckUsageOfDeprecatedNominative`:
 * - 调用标记了 @Deprecated 的声明时发出警告/错误；
 * - 构造调用解析到构造器而构造器自身不带弃用信息时，回退检查其所属 class/struct/enum 的弃用状态。
 *
 * 弃用信息通过声明上的 `deprecationsProvider` 获取，兼容源码与库（序列化）声明。
 */
object CfirDeprecatedCallChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        // 弃用检查消费 resolve 发布的成功引用；错误引用的根因由诊断收集器报告。
        // checker 只依赖公共错误协议，不能跨模块读取 resolve 的候选实现。
        if (expression.calleeReference is CfirDiagnosticHolder) return

        val symbol = expression.resolvedCallableSymbol() ?: return
        val source = expression.calleeReference.source ?: expression.source ?: return
        val lvs = context.languageVersionSettings
        // 构造器的所属类符号（构造器自身无弃用信息时回退用）。
        val declaringClassLike = (symbol.takeIf { it.isBound }?.cfir as? CfirConstructor)
            ?.symbol?.callableId?.classId
            ?.let { context.session.symbolProvider.getClassLikeSymbolByClassId(it) }

        // 直接弃用信息优先；构造器自身无信息时回退到所属类的弃用状态（CheckUsageOfDeprecatedNominative）。
        val deprecation = symbol.getDeprecation(lvs)?.all
            ?: declaringClassLike?.getOwnDeprecation(lvs)?.all
            ?: return

        val isCtor = symbol.takeIf { it.isBound }?.cfir is CfirConstructor
        val kind = if (isCtor) "class" else "function"
        val declName = if (isCtor) {
            declaringClassLike?.classId?.shortClassName ?: symbol.name
        } else {
            symbol.name
        }

        val isError = deprecation.deprecationLevel == DeprecationLevelValue.ERROR
        val factory = if (isError) CfirErrors.DEPRECATED_ERROR else CfirErrors.DEPRECATED_WARNING
        reporter.reportOn(
            source = source,
            factory = factory,
            a = kind,
            b = declName,
            c = "",
            d = "",
        )
    }

    /** 从函数调用 calleeReference 中提取已经解析到的 callable symbol。 */
    private fun CfirFunctionCall.resolvedCallableSymbol(): CfirCallableSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
            else -> null
        }
    }
}
