

package org.cangnova.cangjie.cfir.analysis.diagnostics

import com.intellij.psi.PsiElement
import kotlin.Boolean
import kotlin.String
import kotlin.collections.Collection
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.analysis.diagnostics.*
import org.cangnova.cangjie.cfir.diagnostics.*
import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjTypeReference

/** Generated from: org.cangnova.cangjie.cfir.checkers.generator.diagnostics.DIAGNOSTICS_LIST */
@Suppress("IncorrectFormatting")
object CfirErrors : CjDiagnosticsContainer() {
    // Resolve

    // Imports
    val IMPORT_TARGET_NOT_FOUND: CjDiagnosticFactory1<FqName?> = CjDiagnosticFactory1("CFIR_IMPORT_TARGET_NOT_FOUND", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjImportItem::class, getRendererFactory())
    val IMPORT_CONFLICT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_IMPORT_CONFLICT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjImportItem::class, getRendererFactory())
    val IMPORT_ALIAS_CONFLICT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_IMPORT_ALIAS_CONFLICT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjImportItem::class, getRendererFactory())

    // SuperTypes
    val SUPER_TYPES_SELF_REFERENCE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_SUPER_TYPES_SELF_REFERENCE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val SUPER_TYPES_DUPLICATE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_SUPER_TYPES_DUPLICATE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val INTERFACE_CANNOT_INHERIT_CLASS: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_INTERFACE_CANNOT_INHERIT_CLASS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val MULTIPLE_CLASS_SUPER_TYPES: CjDiagnosticFactory2<Name, Collection<Name>> = CjDiagnosticFactory2("CFIR_MULTIPLE_CLASS_SUPER_TYPES", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())

    // Extend
    val ILLEGAL_EXTENDED_TYPE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_ILLEGAL_EXTENDED_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val EXTEND_DUPLICATE_INTERFACE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_EXTEND_DUPLICATE_INTERFACE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val EXTEND_NOT_INTERFACE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_EXTEND_NOT_INTERFACE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val EXTEND_ORPHAN_RULE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_EXTEND_ORPHAN_RULE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val EXTEND_GENERIC_USAGE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_EXTEND_GENERIC_USAGE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())
    val EXTEND_SPECIALIZATION_CONFLICT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_EXTEND_SPECIALIZATION_CONFLICT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val EXTEND_DEFAULT_IMPLEMENTATION_CONFLICT: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_EXTEND_DEFAULT_IMPLEMENTATION_CONFLICT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())

    // DeclarationStatus
    val STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE: CjDiagnosticFactory1<Name?> = CjDiagnosticFactory1("CFIR_STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())
    val MUT_ONLY_ON_FUNCTION: CjDiagnosticFactory1<Name?> = CjDiagnosticFactory1("CFIR_MUT_ONLY_ON_FUNCTION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())

    // Match
    val NON_EXHAUSTIVE_MATCH: CjDiagnosticFactory1<Collection<String>> = CjDiagnosticFactory1("CFIR_NON_EXHAUSTIVE_MATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Constraint
    val CANNOT_INFER_PARAMETER_TYPE: CjDiagnosticFactory1<CfirTypeParameterSymbol> = CjDiagnosticFactory1("CFIR_CANNOT_INFER_PARAMETER_TYPE", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, CjElement::class, getRendererFactory())

    // TypeCheck
    val TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> = CjDiagnosticFactory3("CFIR_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val PATTERN_INITIALIZER_TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> = CjDiagnosticFactory3("CFIR_PATTERN_INITIALIZER_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.PATTERN_VARIABLE_INITIALIZER, CjNamedDeclaration::class, getRendererFactory())
    val RETURN_TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> = CjDiagnosticFactory3("CFIR_RETURN_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjExpression::class, getRendererFactory())
    val ARGUMENT_TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> = CjDiagnosticFactory3("CFIR_ARGUMENT_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ASSIGNMENT_TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> = CjDiagnosticFactory3("CFIR_ASSIGNMENT_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.OPERATOR, CjExpression::class, getRendererFactory())
    val GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INVISIBLE_MEMBER: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_INVISIBLE_MEMBER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVISIBLE_REFERENCE: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_INVISIBLE_REFERENCE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OVERRIDING_RETURN_TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Name> = CjDiagnosticFactory3("CFIR_OVERRIDING_RETURN_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CANNOT_OVERRIDE_INVISIBLE_MEMBER: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_CANNOT_OVERRIDE_INVISIBLE_MEMBER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CLASS_NOT_OPEN_FOR_INHERITANCE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_CLASS_NOT_OPEN_FOR_INHERITANCE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // ConstEval
    val LITERAL_NUMERIC_OVERFLOW: CjDiagnosticFactory2<String, ConeCangJieType> = CjDiagnosticFactory2("CFIR_LITERAL_NUMERIC_OVERFLOW", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CONST_EVAL_DIVIDE_BY_ZERO: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_CONST_EVAL_DIVIDE_BY_ZERO", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CONST_EVAL_ARITHMETIC_OVERFLOW: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_CONST_EVAL_ARITHMETIC_OVERFLOW", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Unresolved
    val UNRESOLVED_REFERENCE: CjDiagnosticFactory2<String, String?> = CjDiagnosticFactory2("CFIR_UNRESOLVED_REFERENCE", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = CfirErrorsDefaultMessages
}
