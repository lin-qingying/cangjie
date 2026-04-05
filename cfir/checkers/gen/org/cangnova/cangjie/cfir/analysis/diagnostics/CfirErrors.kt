

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
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.lexer.CjKeywordToken
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
    val NO_CONSTRUCTOR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_NO_CONSTRUCTOR", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())

    // Redeclaration
    val CONFLICTING_OVERLOADS: CjDiagnosticFactory1<Collection<String>> = CjDiagnosticFactory1("CFIR_CONFLICTING_OVERLOADS", Severity.ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, CjNamedDeclaration::class, getRendererFactory())
    val REDECLARATION: CjDiagnosticFactory1<Collection<String>> = CjDiagnosticFactory1("CFIR_REDECLARATION", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())
    val CLASSIFIER_REDECLARATION: CjDiagnosticFactory1<Collection<String>> = CjDiagnosticFactory1("CFIR_CLASSIFIER_REDECLARATION", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())

    // Imports
    val UNRESOLVED_IMPORT: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_UNRESOLVED_IMPORT", Severity.ERROR, SourceElementPositioningStrategies.IMPORT_LAST_NAME, PsiElement::class, getRendererFactory())
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
    val EXTEND_IMMUTABLE_MUT_INTERFACE: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_EXTEND_IMMUTABLE_MUT_INTERFACE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val EXTEND_IMMUTABLE_MUT_PROPERTY: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_EXTEND_IMMUTABLE_MUT_PROPERTY", Severity.ERROR, SourceElementPositioningStrategies.MUT_MODIFIER, CjDeclaration::class, getRendererFactory())
    val EXTEND_IMMUTABLE_INDEX_ASSIGNMENT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_EXTEND_IMMUTABLE_INDEX_ASSIGNMENT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())
    val EXTEND_INTERFACE_NOT_EXTENDABLE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_EXTEND_INTERFACE_NOT_EXTENDABLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val EXTEND_C_TYPE_NOT_ALLOWED: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_EXTEND_C_TYPE_NOT_ALLOWED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val EXTEND_SUPER_NOT_ALLOWED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_EXTEND_SUPER_NOT_ALLOWED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjExpression::class, getRendererFactory())
    val STRUCT_SUPER_NOT_ALLOWED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_STRUCT_SUPER_NOT_ALLOWED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjExpression::class, getRendererFactory())
    val ENUM_SUPER_NOT_ALLOWED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_ENUM_SUPER_NOT_ALLOWED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjExpression::class, getRendererFactory())
    val INTERFACE_SUPER_NOT_ALLOWED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INTERFACE_SUPER_NOT_ALLOWED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjExpression::class, getRendererFactory())

    // DeclarationStatus
    val STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE: CjDiagnosticFactory1<Name?> = CjDiagnosticFactory1("CFIR_STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())
    val MUT_ONLY_ON_FUNCTION: CjDiagnosticFactory1<Name?> = CjDiagnosticFactory1("CFIR_MUT_ONLY_ON_FUNCTION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())
    val NOTHING_TO_OVERRIDE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_NOTHING_TO_OVERRIDE", Severity.ERROR, SourceElementPositioningStrategies.OVERRIDE_MODIFIER, CjNamedDeclaration::class, getRendererFactory())
    val OVERRIDE_STATIC_ERROR: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OVERRIDE_STATIC_ERROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val REDEF_INSTANCE_ERROR: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_REDEF_INSTANCE_ERROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_OPERATOR_PARAMETER_COUNT: CjDiagnosticFactory3<String, String, String> = CjDiagnosticFactory3("CFIR_INVALID_OPERATOR_PARAMETER_COUNT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val REPEATED_MODIFIER: CjDiagnosticFactory1<CjKeywordToken> = CjDiagnosticFactory1("CFIR_REPEATED_MODIFIER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val REDUNDANT_MODIFIER: CjDiagnosticFactory2<CjKeywordToken, CjKeywordToken> = CjDiagnosticFactory2("CFIR_REDUNDANT_MODIFIER", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INCOMPATIBLE_MODIFIERS: CjDiagnosticFactory2<CjKeywordToken, CjKeywordToken> = CjDiagnosticFactory2("CFIR_INCOMPATIBLE_MODIFIERS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val WRONG_MODIFIER_TARGET: CjDiagnosticFactory2<CjKeywordToken, String> = CjDiagnosticFactory2("CFIR_WRONG_MODIFIER_TARGET", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val WRONG_MODIFIER_CONTAINING_DECLARATION: CjDiagnosticFactory2<CjKeywordToken, String> = CjDiagnosticFactory2("CFIR_WRONG_MODIFIER_CONTAINING_DECLARATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val REDUNDANT_MODIFIER_FOR_TARGET: CjDiagnosticFactory2<CjKeywordToken, String> = CjDiagnosticFactory2("CFIR_REDUNDANT_MODIFIER_FOR_TARGET", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_MODIFIER_FOR_TARGET: CjDiagnosticFactory2<CjKeywordToken, String> = CjDiagnosticFactory2("CFIR_DEPRECATED_MODIFIER_FOR_TARGET", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_MODIFIER_CONTAINING_DECLARATION: CjDiagnosticFactory2<CjKeywordToken, String> = CjDiagnosticFactory2("CFIR_DEPRECATED_MODIFIER_CONTAINING_DECLARATION", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_MODIFIER_PAIR: CjDiagnosticFactory2<CjKeywordToken, CjKeywordToken> = CjDiagnosticFactory2("CFIR_DEPRECATED_MODIFIER_PAIR", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CANNOT_WEAKEN_ACCESS_PRIVILEGE: CjDiagnosticFactory2<Name, Visibility> = CjDiagnosticFactory2("CFIR_CANNOT_WEAKEN_ACCESS_PRIVILEGE", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())

    // Match
    val NON_EXHAUSTIVE_MATCH: CjDiagnosticFactory1<Collection<String>> = CjDiagnosticFactory1("CFIR_NON_EXHAUSTIVE_MATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Constraint
    val NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val ONLY_ONE_CLASS_BOUND_ALLOWED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_ONLY_ONE_CLASS_BOUND_ALLOWED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjElement::class, getRendererFactory())
    val REPEATED_BOUND: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_REPEATED_BOUND", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjElement::class, getRendererFactory())
    val CONFLICTING_UPPER_BOUNDS: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CONFLICTING_UPPER_BOUNDS", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())
    val CANNOT_INFER_PARAMETER_TYPE: CjDiagnosticFactory1<CfirTypeParameterSymbol> = CjDiagnosticFactory1("CFIR_CANNOT_INFER_PARAMETER_TYPE", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, CjElement::class, getRendererFactory())
    val NEW_INFERENCE_ERROR: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_NEW_INFERENCE_ERROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR: CjDiagnosticFactory1<CfirTypeParameterSymbol> = CjDiagnosticFactory1("CFIR_TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, CjElement::class, getRendererFactory())
    val BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION: CjDiagnosticFactory4<String, Collection<ConeCangJieType>, String, String> = CjDiagnosticFactory4("CFIR_INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION: CjDiagnosticFactory4<String, Collection<ConeCangJieType>, String, String> = CjDiagnosticFactory4("CFIR_INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // TypeCheck
    val TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> = CjDiagnosticFactory3("CFIR_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val PATTERN_INITIALIZER_TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> = CjDiagnosticFactory3("CFIR_PATTERN_INITIALIZER_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.PATTERN_VARIABLE_INITIALIZER, CjNamedDeclaration::class, getRendererFactory())
    val RETURN_TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> = CjDiagnosticFactory3("CFIR_RETURN_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjExpression::class, getRendererFactory())
    val ARGUMENT_TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> = CjDiagnosticFactory3("CFIR_ARGUMENT_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ASSIGNMENT_TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> = CjDiagnosticFactory3("CFIR_ASSIGNMENT_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.OPERATOR, CjExpression::class, getRendererFactory())
    val GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INVISIBLE_MEMBER: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_INVISIBLE_MEMBER", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INVISIBLE_REFERENCE: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_INVISIBLE_REFERENCE", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val OVERRIDING_RETURN_TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Name> = CjDiagnosticFactory3("CFIR_OVERRIDING_RETURN_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CANNOT_OVERRIDE_INVISIBLE_MEMBER: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_CANNOT_OVERRIDE_INVISIBLE_MEMBER", Severity.ERROR, SourceElementPositioningStrategies.OVERRIDE_MODIFIER, CjNamedDeclaration::class, getRendererFactory())
    val CLASS_NOT_OPEN_FOR_INHERITANCE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_CLASS_NOT_OPEN_FOR_INHERITANCE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val ABSTRACT_MEMBER_NOT_IMPLEMENTED: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_ABSTRACT_MEMBER_NOT_IMPLEMENTED", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())

    // ConstEval
    val LITERAL_NUMERIC_OVERFLOW: CjDiagnosticFactory2<String, ConeCangJieType> = CjDiagnosticFactory2("CFIR_LITERAL_NUMERIC_OVERFLOW", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CONST_EVAL_DIVIDE_BY_ZERO: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_CONST_EVAL_DIVIDE_BY_ZERO", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CONST_EVAL_ARITHMETIC_OVERFLOW: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_CONST_EVAL_ARITHMETIC_OVERFLOW", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Unresolved
    val UNRESOLVED_REFERENCE: CjDiagnosticFactory2<String, String?> = CjDiagnosticFactory2("CFIR_UNRESOLVED_REFERENCE", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INVALID_BINARY_OPERATOR: CjDiagnosticFactory3<String, String, String> = CjDiagnosticFactory3("CFIR_INVALID_BINARY_OPERATOR", Severity.ERROR, SourceElementPositioningStrategies.OPERATOR, PsiElement::class, getRendererFactory())
    val NO_MATCHING_OPERATOR_INVOKE: CjDiagnosticFactory2<String, ConeCangJieType> = CjDiagnosticFactory2("CFIR_NO_MATCHING_OPERATOR_INVOKE", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = CfirErrorsDefaultMessages
}
