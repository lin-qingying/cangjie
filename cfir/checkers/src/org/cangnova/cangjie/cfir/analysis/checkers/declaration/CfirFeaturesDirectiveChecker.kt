/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builtInDescriptor

/**
 * 文件前导 `features` 的注解语义 owner。
 *
 * 官方 parser 只允许 `@NonProduct` 出现在 FeaturesDirective 上；该规则
 * 不能由 declaration checker 代替，因为 directive 不属于 declarations。
 */
object CfirFeaturesDirectiveChecker : CfirFileChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        val featuresDirective = declaration.featuresDirective ?: return
        for (annotation in featuresDirective.annotations.filterIsInstance<CfirAnnotationCall>()) {
            val descriptor = annotation.builtInDescriptor ?: continue
            if (descriptor.kind != BuiltInAnnotationKind.NON_PRODUCT) {
                reporter.reportOn(
                    source = annotation.source ?: featuresDirective.source,
                    factory = CfirErrors.ILLEGAL_USE_OF_ANNOTATION,
                    a = "features directive",
                    b = "@${descriptor.sourceName}",
                )
                continue
            }
            if (annotation.argumentCount() != 0) {
                reporter.reportOn(
                    source = annotation.source,
                    factory = CfirErrors.ANNOTATION_ERROR_ARG_NUM,
                    a = "@NonProduct",
                    b = "no",
                )
            }
        }
    }
}
