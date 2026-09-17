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
            val descriptor = annotation.builtInDescriptor ?: continue
            // ObjC/Java mirror and ForeignName are interop-library annotations,
            // not official language AnnotationKind entries. Their source grammar
            // and constructor ownership are handled by the interop annotation
            // contract; they must not be forced through the language builtin
            // argument-arity checker.
            if (descriptor.kind in NON_LANGUAGE_INTEROP_KINDS) continue
            if (descriptor.kind == org.cangnova.cangjie.annotations.BuiltInAnnotationKind.CALLING_CONV) continue
            val schema = descriptor.argumentSchema
            if (schema.variadic) continue
            val count = annotation.argumentCount()
            val required = schema.parameters.count { it.required }
            val maximum = schema.parameters.size
            if (count in required..maximum) continue
            val firstArgument = annotation.argumentView?.entries?.firstOrNull { it.argument != null }
            reporter.reportOn(
                if (maximum == 0) firstArgument?.source else annotation.source,
                CfirErrors.ANNOTATION_ERROR_ARG_NUM,
                "@"+descriptor.sourceName,
                if (maximum == 0) "no" else "one",
            )
        }
    }

    /** 官方 AnnotationKind 之外的互操作库注解，不能由通用 builtin arity 规则检查。 */
    private val NON_LANGUAGE_INTEROP_KINDS = setOf(
        org.cangnova.cangjie.annotations.BuiltInAnnotationKind.JAVA_MIRROR,
        org.cangnova.cangjie.annotations.BuiltInAnnotationKind.JAVA_IMPL,
        org.cangnova.cangjie.annotations.BuiltInAnnotationKind.JAVA_HAS_DEFAULT,
        org.cangnova.cangjie.annotations.BuiltInAnnotationKind.OBJ_C_MIRROR,
        org.cangnova.cangjie.annotations.BuiltInAnnotationKind.OBJ_C_IMPL,
        org.cangnova.cangjie.annotations.BuiltInAnnotationKind.OBJ_C_INIT,
        org.cangnova.cangjie.annotations.BuiltInAnnotationKind.OBJ_C_OPTIONAL,
        org.cangnova.cangjie.annotations.BuiltInAnnotationKind.FOREIGN_NAME,
        org.cangnova.cangjie.annotations.BuiltInAnnotationKind.FOREIGN_GETTER_NAME,
        org.cangnova.cangjie.annotations.BuiltInAnnotationKind.FOREIGN_SETTER_NAME,
    )
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
