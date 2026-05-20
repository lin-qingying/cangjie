package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall

/**
 * 内置注解参数数量检查器。
 *
 * 对齐 C++ CFFICheck.cpp 多处:
 * - `@C`、`@FastNative`、`@Frozen`:无参数
 * - `@CallingConv`:恰好一个参数
 *
 * 参数数量不匹配时报 `ANNOTATION_ERROR_ARG_NUM`。
 */
object CfirAnnotationArgNumberChecker {
    private val NO_ARG_ANNOTATIONS = mapOf(
        "C" to "no",
        "FastNative" to "no",
        "Frozen" to "no",
    )
    private val ONE_ARG_ANNOTATIONS = mapOf(
        "CallingConv" to "one",
    )

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun check(declaration: CfirDeclaration) {
        for (annotation in declaration.annotations) {
            val shortName = annotation.shortNameOrNull()?.asString() ?: continue
            val actualCount = (annotation as? CfirAnnotationCall)?.argumentList?.arguments?.size ?: 0

            NO_ARG_ANNOTATIONS[shortName]?.let { _ ->
                if (actualCount > 0) {
                    reporter.reportOn(
                        source = annotation.source,
                        factory = CfirErrors.ANNOTATION_ERROR_ARG_NUM,
                        a = "@$shortName",
                        b = "no",
                    )
                }
            }
            ONE_ARG_ANNOTATIONS[shortName]?.let { _ ->
                if (actualCount != 1) {
                    reporter.reportOn(
                        source = annotation.source,
                        factory = CfirErrors.ANNOTATION_ERROR_ARG_NUM,
                        a = "@$shortName",
                        b = "one",
                    )
                }
            }
        }
    }
}

/** 面向 CfirClassLikeDeclaration 的分发。 */
object CfirAnnotationArgNumberClassChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        CfirAnnotationArgNumberChecker.check(declaration)
    }
}

/** 面向 CfirCallableDeclaration 的分发。 */
object CfirAnnotationArgNumberCallableChecker : CfirCallableDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirCallableDeclaration) {
        CfirAnnotationArgNumberChecker.check(declaration)
    }
}
