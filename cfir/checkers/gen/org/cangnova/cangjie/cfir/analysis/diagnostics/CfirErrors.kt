/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.analysis.diagnostics

import com.intellij.psi.PsiElement
import kotlin.String
import org.cangjie.cfir.analysis.diagnostics.*
import org.cangjie.cfir.diagnostics.*
import org.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.cangjie.config.LanguageFeature

/** Generated from: org.cangjie.cfir.checkers.generator.diagnostics.DIAGNOSTICS_LIST */
@Suppress("IncorrectFormatting")
object CfirErrors : CjDiagnosticsContainer() {
    // Resolve
    val INVALID_DECLARATION: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_INVALID_DECLARATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPES_ERROR_RECOVERY: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_TYPES_ERROR_RECOVERY", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val IMPORT_TARGET_NOT_FOUND: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_IMPORT_TARGET_NOT_FOUND", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val IMPORT_CONFLICT: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_IMPORT_CONFLICT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val IMPORT_ALIAS_CONFLICT: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_IMPORT_ALIAS_CONFLICT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SUPER_TYPES_SELF_REFERENCE: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_SUPER_TYPES_SELF_REFERENCE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SUPER_TYPES_DUPLICATE: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_SUPER_TYPES_DUPLICATE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ILLEGAL_EXTENDED_TYPE: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_ILLEGAL_EXTENDED_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXTEND_DUPLICATE_INTERFACE: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_EXTEND_DUPLICATE_INTERFACE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXTEND_NOT_INTERFACE: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_EXTEND_NOT_INTERFACE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INTERFACE_CANNOT_INHERIT_CLASS: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_INTERFACE_CANNOT_INHERIT_CLASS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MULTIPLE_CLASS_SUPER_TYPES: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_MULTIPLE_CLASS_SUPER_TYPES", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val STATUS_MODIFIER_LEGALITY: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_STATUS_MODIFIER_LEGALITY", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = CfirErrorsDefaultMessages
}
