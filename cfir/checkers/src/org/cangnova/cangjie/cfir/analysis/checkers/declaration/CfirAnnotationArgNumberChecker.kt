package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builtInDescriptor
import org.cangnova.cangjie.cfir.expressions.annotationVersionSupport
import org.cangnova.cangjie.cfir.expressions.platformAnnotationDescriptor
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentStatus
import org.cangnova.cangjie.cfir.session.languageVersionSettings

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
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun check(declaration: CfirDeclaration) {
        for (annotation in declaration.annotations.filterIsInstance<CfirAnnotationCall>()) {
            val descriptor = annotation.builtInDescriptor
            val platformDescriptor = annotation.platformAnnotationDescriptor
            if (descriptor == null && platformDescriptor == null) continue
            if (annotation.annotationVersionSupport(context.session.languageVersionSettings) !=
                org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus.SUPPORTED
            ) continue
            val argumentSchema = descriptor?.argumentSchema ?: platformDescriptor!!.argumentSchema
            if (descriptor?.kind == org.cangnova.cangjie.annotations.BuiltInAnnotationKind.CALLING_CONV) continue
            // JavaHasDefault has a dedicated official diagnostic for any
            // argument; its semantic owner must remain the only reporter.
            if (platformDescriptor?.platformKind == org.cangnova.cangjie.annotations.CangjiePlatformAnnotationKind.JAVA_HAS_DEFAULT) continue
            if (argumentSchema.variadic) continue
            val count = annotation.argumentCount()
            val required = argumentSchema.parameters.count { it.required }
            val maximum = argumentSchema.parameters.size
            if (count !in required..maximum) {
                val firstArgument = annotation.argumentView?.entries?.firstOrNull { it.argument != null }
                reporter.reportOn(
                    if (maximum == 0) firstArgument?.source else annotation.source,
                    CfirErrors.ANNOTATION_ERROR_ARG_NUM,
                    "@" + (descriptor?.sourceName ?: platformDescriptor!!.sourceName),
                    if (maximum == 0) "no" else "one",
                )
                continue
            }

            // 平台注解不经过普通 annotation constructor；参数解析 owner 已将
            // 类型/绑定状态发布到 argument view，这里只把错误状态转换为官方
            // annotation 参数类型诊断，不重新解析实参。
            if (platformDescriptor != null) {
                annotation.argumentView?.entries
                    ?.filter { it.status == CfirAnnotationArgumentStatus.ERROR }
                    ?.forEach { entry ->
                        reporter.reportOn(
                            entry.source ?: annotation.source,
                            CfirErrors.ANNOTATION_INVALID_ARGS_TYPE,
                            "@${platformDescriptor.sourceName}",
                        )
                    }
            }
        }
    }

}

/** 面向 CfirClassLikeDeclaration 的分发。 */
object CfirAnnotationArgNumberClassChecker : CfirClassLikeChecker() {
    /** 对 class-like 声明分发内置注解参数数量检查。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        CfirAnnotationArgNumberChecker.check(declaration)
    }
}

/** 面向 CfirCallableDeclaration 的分发。 */
object CfirAnnotationArgNumberCallableChecker : CfirCallableDeclarationChecker() {
    /** 对 callable 声明分发内置注解参数数量检查。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirCallableDeclaration) {
        CfirAnnotationArgNumberChecker.check(declaration)
    }
}
