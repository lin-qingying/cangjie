

package org.cangnova.cangjie.cfir.analysis.diagnostics

import com.intellij.psi.PsiElement
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
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
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCommandTypePattern
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjHandleClause
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjPerformExpression
import org.cangnova.cangjie.psi.CjResumeExpression
import org.cangnova.cangjie.psi.CjTypeReference

/** Generated from: org.cangnova.cangjie.cfir.checkers.generator.diagnostics.DIAGNOSTICS_LIST */
@Suppress("IncorrectFormatting")
object CfirErrors : CjDiagnosticsContainer() {
    // Resolve
    val NO_CONSTRUCTOR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_NO_CONSTRUCTOR", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())

    // Redeclaration
    val CONFLICTING_OVERLOADS: CjDiagnosticFactory1<Collection<String>> = CjDiagnosticFactory1("CFIR_CONFLICTING_OVERLOADS", Severity.ERROR, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, CjNamedDeclaration::class, getRendererFactory())
    val REDECLARATION: CjDiagnosticFactory1<Collection<String>> = CjDiagnosticFactory1("CFIR_REDECLARATION", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())
    val CLASSIFIER_REDECLARATION: CjDiagnosticFactory1<Collection<String>> = CjDiagnosticFactory1("CFIR_CLASSIFIER_REDECLARATION", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())

    // Imports
    val UNRESOLVED_IMPORT: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_UNRESOLVED_IMPORT", Severity.ERROR, SourceElementPositioningStrategies.IMPORT_LAST_NAME, PsiElement::class, getRendererFactory())
    val IMPORT_CONFLICT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_IMPORT_CONFLICT", Severity.ERROR, SourceElementPositioningStrategies.IMPORT_LAST_NAME, CjImportItem::class, getRendererFactory())
    val IMPORT_ALIAS_CONFLICT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_IMPORT_ALIAS_CONFLICT", Severity.ERROR, SourceElementPositioningStrategies.IMPORT_ALIAS, CjImportItem::class, getRendererFactory())

    // SuperTypes
    val SUPER_TYPES_SELF_REFERENCE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_SUPER_TYPES_SELF_REFERENCE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val SUPER_TYPES_DUPLICATE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_SUPER_TYPES_DUPLICATE", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())
    val INTERFACE_CANNOT_INHERIT_CLASS: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_INTERFACE_CANNOT_INHERIT_CLASS", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())
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
    val PARAM_NAMED_MISMATCHED: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_PARAM_NAMED_MISMATCHED", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())

    // CallResolution
    val NO_VALUE_FOR_PARAMETER: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_NO_VALUE_FOR_PARAMETER", Severity.ERROR, SourceElementPositioningStrategies.VALUE_ARGUMENTS_LIST, PsiElement::class, getRendererFactory())
    val TOO_MANY_ARGUMENTS: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_TOO_MANY_ARGUMENTS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NAMED_PARAMETER_NOT_FOUND: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_NAMED_PARAMETER_NOT_FOUND", Severity.ERROR, SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT, PsiElement::class, getRendererFactory())
    val ARGUMENT_PASSED_TWICE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_ARGUMENT_PASSED_TWICE", Severity.ERROR, SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT, PsiElement::class, getRendererFactory())
    val NAMED_ARGUMENTS_NOT_ALLOWED: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_NAMED_ARGUMENTS_NOT_ALLOWED", Severity.ERROR, SourceElementPositioningStrategies.NAME_OF_NAMED_ARGUMENT, PsiElement::class, getRendererFactory())
    val MIXING_NAMED_AND_POSITIONAL_ARGUMENTS: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_MIXING_NAMED_AND_POSITIONAL_ARGUMENTS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NEED_NAMED_ARGUMENT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_NEED_NAMED_ARGUMENT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val AMBIGUOUS_CONSTRUCTOR_CALL: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_AMBIGUOUS_CONSTRUCTOR_CALL", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val AMBIGUOUS_FUNCTION_CALL: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_AMBIGUOUS_FUNCTION_CALL", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val RECURSIVE_CONSTRUCTOR_CALL: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_RECURSIVE_CONSTRUCTOR_CALL", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val ILLEGAL_THIS_OR_SUPER_CALL: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_ILLEGAL_THIS_OR_SUPER_CALL", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val EXPLICIT_SUPER_CALL_REQUIRED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_EXPLICIT_SUPER_CALL_REQUIRED", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val INVALID_LOOP_CONTROL: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INVALID_LOOP_CONTROL", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Initialization
    val USED_BEFORE_INITIALIZATION: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_USED_BEFORE_INITIALIZATION", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val CLASS_UNINITIALIZED_FIELD: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_CLASS_UNINITIALIZED_FIELD", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, PsiElement::class, getRendererFactory())

    // GenericAccess
    val GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())

    // Mutability
    val CANNOT_MODIFY_VAR: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_CANNOT_MODIFY_VAR", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())

    // Annotation
    val ANNOTATION_NO_CONST_INIT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_ANNOTATION_NO_CONST_INIT", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, PsiElement::class, getRendererFactory())

    // Interop
    val INVALID_CFUNC_RETURN_TYPE: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_INVALID_CFUNC_RETURN_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val INVALID_CFUNC_PARAMETER_TYPE: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_INVALID_CFUNC_PARAMETER_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val ONLY_CFUNC_CAN_USE_ANNOTATION: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_ONLY_CFUNC_CAN_USE_ANNOTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ILLEGAL_SCOPE_USE_OF_ANNOTATION: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_ILLEGAL_SCOPE_USE_OF_ANNOTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Exception
    val THROW_EXPR_WITH_WRONG_TYPE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_THROW_EXPR_WITH_WRONG_TYPE", Severity.ERROR, SourceElementPositioningStrategies.THROW_KEYWORD, PsiElement::class, getRendererFactory())
    val CATCH_TYPE_MUST_EXTEND_EXCEPTION: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CATCH_TYPE_MUST_EXTEND_EXCEPTION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val USELESS_EXCEPTION_TYPE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_USELESS_EXCEPTION_TYPE", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())

    // Range
    val RANGE_STEP_CANNOT_BE_ZERO: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_RANGE_STEP_CANNOT_BE_ZERO", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Effects
    val EFFECTS_FEATURE_DISABLED: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_EFFECTS_FEATURE_DISABLED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjElement::class, getRendererFactory())
    val COMMAND_INCOMPATIBLE_TYPE: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_COMMAND_INCOMPATIBLE_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjPerformExpression::class, getRendererFactory())
    val COMMAND_HANDLE_TYPE_ERROR: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_COMMAND_HANDLE_TYPE_ERROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjCommandTypePattern::class, getRendererFactory())
    val IMPLICIT_RESUME_OUTSIDE_HANDLER: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_IMPLICIT_RESUME_OUTSIDE_HANDLER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjResumeExpression::class, getRendererFactory())
    val RESUME_NO_WITH: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_RESUME_NO_WITH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjResumeExpression::class, getRendererFactory())
    val RESUME_THROWING_MISMATCH_TYPE: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_RESUME_THROWING_MISMATCH_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjResumeExpression::class, getRendererFactory())
    val MISMATCHING_HANDLE_BLOCK: CjDiagnosticFactory2<ConeCangJieType, ConeCangJieType> = CjDiagnosticFactory2("CFIR_MISMATCHING_HANDLE_BLOCK", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjHandleClause::class, getRendererFactory())

    // Match
    val NON_EXHAUSTIVE_MATCH: CjDiagnosticFactory1<Collection<String>> = CjDiagnosticFactory1("CFIR_NON_EXHAUSTIVE_MATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TUPLE_PATTERN_NOT_MATCH: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_TUPLE_PATTERN_NOT_MATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val PATTERN_NOT_MATCH: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_PATTERN_NOT_MATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ENUM_PATTERN_PARAM_SIZE_ERROR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_ENUM_PATTERN_PARAM_SIZE_ERROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NOT_OVERLOAD_IN_MATCH: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_NOT_OVERLOAD_IN_MATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MATCH_CASE_HAS_NO_TYPE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_MATCH_CASE_HAS_NO_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

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
    val VARRAY_SIZE_MISMATCH: CjDiagnosticFactory3<Long, Long, ConeCangJieType> = CjDiagnosticFactory3("CFIR_VARRAY_SIZE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INVISIBLE_MEMBER: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_INVISIBLE_MEMBER", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INVISIBLE_REFERENCE: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_INVISIBLE_REFERENCE", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val OVERRIDING_RETURN_TYPE_MISMATCH: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Name> = CjDiagnosticFactory3("CFIR_OVERRIDING_RETURN_TYPE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, PsiElement::class, getRendererFactory())
    val CANNOT_OVERRIDE_INVISIBLE_MEMBER: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_CANNOT_OVERRIDE_INVISIBLE_MEMBER", Severity.ERROR, SourceElementPositioningStrategies.OVERRIDE_MODIFIER, CjNamedDeclaration::class, getRendererFactory())
    val CLASS_NOT_OPEN_FOR_INHERITANCE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_CLASS_NOT_OPEN_FOR_INHERITANCE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val ABSTRACT_MEMBER_NOT_IMPLEMENTED: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_ABSTRACT_MEMBER_NOT_IMPLEMENTED", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())

    // ConstEval
    val LITERAL_NUMERIC_OVERFLOW: CjDiagnosticFactory2<String, ConeCangJieType> = CjDiagnosticFactory2("CFIR_LITERAL_NUMERIC_OVERFLOW", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CONST_EVAL_DIVIDE_BY_ZERO: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_CONST_EVAL_DIVIDE_BY_ZERO", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CONST_EVAL_ARITHMETIC_OVERFLOW: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_CONST_EVAL_ARITHMETIC_OVERFLOW", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CONST_EVAL_NEGATIVE_SHIFT_COUNT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CONST_EVAL_NEGATIVE_SHIFT_COUNT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CONST_EVAL_SHIFT_COUNT_OVERFLOW: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CONST_EVAL_SHIFT_COUNT_OVERFLOW", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Unresolved
    val UNRESOLVED_REFERENCE: CjDiagnosticFactory2<String, String?> = CjDiagnosticFactory2("CFIR_UNRESOLVED_REFERENCE", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val INVALID_BINARY_OPERATOR: CjDiagnosticFactory3<String, String, String> = CjDiagnosticFactory3("CFIR_INVALID_BINARY_OPERATOR", Severity.ERROR, SourceElementPositioningStrategies.OPERATOR, PsiElement::class, getRendererFactory())
    val NO_MATCHING_OPERATOR_INVOKE: CjDiagnosticFactory2<String, ConeCangJieType> = CjDiagnosticFactory2("CFIR_NO_MATCHING_OPERATOR_INVOKE", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())

    // General
    val INVALID_NODE_AFTER_CHECK: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INVALID_NODE_AFTER_CHECK", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UNABLE_TO_INFER_DECL: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_UNABLE_TO_INFER_DECL", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MISMATCHED_TYPES_MULTIPLE_ASSIGN: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_MISMATCHED_TYPES_MULTIPLE_ASSIGN", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MISMATCHED_TYPES_BECAUSE: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, String> = CjDiagnosticFactory3("CFIR_MISMATCHED_TYPES_BECAUSE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val AMBIGUOUS_USE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_AMBIGUOUS_USE", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val CONFLICT_WITH_SUB_PACKAGE: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_CONFLICT_WITH_SUB_PACKAGE", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())
    val CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ACCESSIBILITY_WITH_MAIN_HINT: CjDiagnosticFactory3<String, Name, Visibility> = CjDiagnosticFactory3("CFIR_ACCESSIBILITY_WITH_MAIN_HINT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ACCESSIBILITY_ERROR: CjDiagnosticFactory2<String, Visibility> = CjDiagnosticFactory2("CFIR_ACCESSIBILITY_ERROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val PARAM_COUNT_MISMATCH: CjDiagnosticFactory2<Int, Int> = CjDiagnosticFactory2("CFIR_PARAM_COUNT_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Function
    val UNABLE_TO_INFER_RETURN_TYPE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_UNABLE_TO_INFER_RETURN_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())
    val UNABLE_TO_INFER_GENERIC_FUNC: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_UNABLE_TO_INFER_GENERIC_FUNC", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_CALLED_OBJECT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INVALID_CALLED_OBJECT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_RETURN: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INVALID_RETURN", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_RETURN_IN_STATIC_INIT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INVALID_RETURN_IN_STATIC_INIT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_SUBSCRIPT_ASSIGN_PARAMETER: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INVALID_SUBSCRIPT_ASSIGN_PARAMETER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())
    val INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())
    val INVALID_SUBSCRIPT_ASSIGN_RETURN: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INVALID_SUBSCRIPT_ASSIGN_RETURN", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())
    val STATIC_FUNCTION_OVERLOAD_CONFLICTS: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_STATIC_FUNCTION_OVERLOAD_CONFLICTS", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())
    val USE_MUTABLE_FUNC_ALONE: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_USE_MUTABLE_FUNC_ALONE", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val UNSAFE_FUNC_CAN_ONLY_BE_CALLED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_UNSAFE_FUNC_CAN_ONLY_BE_CALLED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val AMBIGUOUS_MATCH_PRIMITIVE_EXTEND: CjDiagnosticFactory2<Name, Collection<Name>> = CjDiagnosticFactory2("CFIR_AMBIGUOUS_MATCH_PRIMITIVE_EXTEND", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CANNOT_HAVE_DEFAULT_PARAM: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_CANNOT_HAVE_DEFAULT_PARAM", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val LAMBDA_MUST_HAVE_TYPE_ANNOTATION: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_LAMBDA_MUST_HAVE_TYPE_ANNOTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val USE_FUNC_CAPTURE_VAR_ALONE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_USE_FUNC_CAPTURE_VAR_ALONE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Expression
    val UNABLE_TO_INFER_EXPR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_UNABLE_TO_INFER_EXPR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXCEED_FLOAT_LITERAL_RANGE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_EXCEED_FLOAT_LITERAL_RANGE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val FLOAT_LITERAL_TOO_LARGE: CjDiagnosticFactory2<ConeCangJieType, String> = CjDiagnosticFactory2("CFIR_FLOAT_LITERAL_TOO_LARGE", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val FLOAT_LITERAL_TOO_SMALL: CjDiagnosticFactory2<ConeCangJieType, String> = CjDiagnosticFactory2("CFIR_FLOAT_LITERAL_TOO_SMALL", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_UNARY_EXPR: CjDiagnosticFactory2<String, ConeCangJieType> = CjDiagnosticFactory2("CFIR_INVALID_UNARY_EXPR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_UNARY_EXPR_WITH_TARGET: CjDiagnosticFactory3<String, ConeCangJieType, ConeCangJieType> = CjDiagnosticFactory3("CFIR_INVALID_UNARY_EXPR_WITH_TARGET", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_SUBSCRIPT_EXPR: CjDiagnosticFactory2<ConeCangJieType, String> = CjDiagnosticFactory2("CFIR_INVALID_SUBSCRIPT_EXPR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CANNOT_ASSIGN_TO_SUBSCRIPT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CANNOT_ASSIGN_TO_SUBSCRIPT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NOT_MEMBER_OF: CjDiagnosticFactory3<Name, String, Name> = CjDiagnosticFactory3("CFIR_NOT_MEMBER_OF", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val MEMBER_NOT_IMPORTED: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_MEMBER_NOT_IMPORTED", Severity.ERROR, SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED, PsiElement::class, getRendererFactory())
    val CANNOT_ASSIGN_TO_IMMUTABLE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CANNOT_ASSIGN_TO_IMMUTABLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UNQUALIFIED_LEFT_VALUE_ASSIGNED: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_UNQUALIFIED_LEFT_VALUE_ASSIGNED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DIFFERENT_OR_PATTERN: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_DIFFERENT_OR_PATTERN", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VAR_IN_OR_PATTERN: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_VAR_IN_OR_PATTERN", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VAR_IN_OR_CONDITION: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_VAR_IN_OR_CONDITION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UNREACHABLE_PATTERN: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_UNREACHABLE_PATTERN", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OPTIONAL_CHAIN_NON_OPTIONAL: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_OPTIONAL_CHAIN_NON_OPTIONAL", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CAPTURE_BEFORE_INITIALIZATION: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_CAPTURE_BEFORE_INITIALIZATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INTERPOLATION_IN_CONST_PATTERN: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INTERPOLATION_IN_CONST_PATTERN", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CANNOT_REF_TO_PKG_NAME: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CANNOT_REF_TO_PKG_NAME", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val USE_EXPR_WITHOUT_IMPORT: CjDiagnosticFactory2<FqName, String> = CjDiagnosticFactory2("CFIR_USE_EXPR_WITHOUT_IMPORT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // GenericDeep
    val GENERIC_TYPE_INCONSISTENT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_GENERIC_TYPE_INCONSISTENT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val GENERIC_ARGUMENT_NO_MATCH: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_GENERIC_ARGUMENT_NO_MATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val GENERIC_CONSTRAINT_NOT_LOOSER: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_GENERIC_CONSTRAINT_NOT_LOOSER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY: CjDiagnosticFactory2<Name, ConeCangJieType> = CjDiagnosticFactory2("CFIR_GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val GENERIC_PARAM_DIRECTLY_RECURSIVE: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_GENERIC_PARAM_DIRECTLY_RECURSIVE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE: CjDiagnosticFactory2<ConeCangJieType, Name> = CjDiagnosticFactory2("CFIR_UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val GENERIC_STATIC_ACCESS: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_GENERIC_STATIC_ACCESS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val PRIMITIVE_TYPE_AS_GENERICS_ARG: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_PRIMITIVE_TYPE_AS_GENERICS_ARG", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MEET_CONSTRAINT_INDIRECTLY: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_MEET_CONSTRAINT_INDIRECTLY", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val GENERIC_UPPER_BOUNDS_MUST_BE_JAVA_IN_JAVA: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_GENERIC_UPPER_BOUNDS_MUST_BE_JAVA_IN_JAVA", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // InheritanceDeep
    val INHERIT_MEMBER_KIND_INCONSISTENT: CjDiagnosticFactory4<String, Name, String, Name> = CjDiagnosticFactory4("CFIR_INHERIT_MEMBER_KIND_INCONSISTENT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INHERIT_SUPER_MEMBER_KIND_INCONSISTENT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_INHERIT_SUPER_MEMBER_KIND_INCONSISTENT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INHERIT_MEMBER_TYPE_INCONSISTENT: CjDiagnosticFactory3<String, String, Name> = CjDiagnosticFactory3("CFIR_INHERIT_MEMBER_TYPE_INCONSISTENT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC: CjDiagnosticFactory3<Name, String, Name> = CjDiagnosticFactory3("CFIR_INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_MEMBER_VISIBILITY_IN_CLASS: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_INVALID_MEMBER_VISIBILITY_IN_CLASS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CANNOT_INHERIT_SEALED: CjDiagnosticFactory4<String, String, String, Name> = CjDiagnosticFactory4("CFIR_CANNOT_INHERIT_SEALED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjTypeReference::class, getRendererFactory())
    val INHERIT_THREAD_CONTEXT_INVALID: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_INHERIT_THREAD_CONTEXT_INVALID", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INHERIT_THREAD_CONTEXT_NOT_OPEN: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_INHERIT_THREAD_CONTEXT_NOT_OPEN", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INHERIT_NOT_RETURN_THIS: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INHERIT_NOT_RETURN_THIS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Spawn
    val SPAWN_ARG_INVALID: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_SPAWN_ARG_INVALID", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPAWN_ARG_NO_EFFECT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_SPAWN_ARG_NO_EFFECT", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Interface
    val INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL: CjDiagnosticFactory2<String, Name> = CjDiagnosticFactory2("CFIR_INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // ClassStructSemantics
    val TYPE_UNINITIALIZED_STATIC_FIELD: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_TYPE_UNINITIALIZED_STATIC_FIELD", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NON_ABSTRACT_CLASS_CANNOT_BE_SEALED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_NON_ABSTRACT_CLASS_CANNOT_BE_SEALED", Severity.ERROR, SourceElementPositioningStrategies.ACTUAL_DECLARATION_NAME, CjNamedDeclaration::class, getRendererFactory())
    val STATIC_VARIABLE_USE_GENERIC_PARAMETER: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_STATIC_VARIABLE_USE_GENERIC_PARAMETER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CSTRUCT_CANNOT_IMPL_INTERFACES: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CSTRUCT_CANNOT_IMPL_INTERFACES", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXPORT_SAME_PRIVATE_DECL: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_EXPORT_SAME_PRIVATE_DECL", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // ExtendExtra
    val EXTEND_FUNCTION_CANNOT_OVERRIDDEN: CjDiagnosticFactory2<String, Name> = CjDiagnosticFactory2("CFIR_EXTEND_FUNCTION_CANNOT_OVERRIDDEN", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXTEND_MEMBER_CANNOT_SHADOW: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_EXTEND_MEMBER_CANNOT_SHADOW", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXTEND_ILLEGAL_MEMBER: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_EXTEND_ILLEGAL_MEMBER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND: CjDiagnosticFactory1<Collection<Name>> = CjDiagnosticFactory1("CFIR_EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXTEND_A_JAVA_TYPE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_EXTEND_A_JAVA_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXTEND_REF_TARGET_CANNOT_BE_JAVA_IMPL: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_EXTEND_REF_TARGET_CANNOT_BE_JAVA_IMPL", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE: CjDiagnosticFactory2<String, Name> = CjDiagnosticFactory2("CFIR_TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Property
    val PROPERTY_MUST_HAVE_ACCESSORS: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_PROPERTY_MUST_HAVE_ACCESSORS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())
    val IMMUTABLE_PROPERTY_WITH_SETTER: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_IMMUTABLE_PROPERTY_WITH_SETTER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, CjDeclaration::class, getRendererFactory())
    val PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val PROPERTY_MUST_IMPLEMENT_BOTH: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_PROPERTY_MUST_IMPLEMENT_BOTH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // ConstDeclaration
    val EXPECT_CONST: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_EXPECT_CONST", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CANNOT_DEFINE_VAR_IN_CONST_FUNCTION: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CANNOT_DEFINE_VAR_IN_CONST_FUNCTION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NO_CONST_INIT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_NO_CONST_INIT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CLASS_CONST_INIT_WITH_VAR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CLASS_CONST_INIT_WITH_VAR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // AnnotationExtra
    val ANNOTATION_ARG_TARGET: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_ANNOTATION_ARG_TARGET", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ANNOTATION_ARG_TARGET_ARRAY_LIT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_ANNOTATION_ARG_TARGET_ARRAY_LIT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ANNOTATION_NON_PUBLIC: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_ANNOTATION_NON_PUBLIC", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ANNOTATION_CUSTOM_PLACE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_ANNOTATION_CUSTOM_PLACE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ANNOTATION_ERROR_ARG_NUM: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_ANNOTATION_ERROR_ARG_NUM", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ANNOTATION_ERROR_ARG_RANGE: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_ANNOTATION_ERROR_ARG_RANGE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ANNOTATION_ERROR_OBJECT: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_ANNOTATION_ERROR_OBJECT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CANNOT_USE_ANNOTATION_JFFI: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CANNOT_USE_ANNOTATION_JFFI", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ANNOTATION_NOT_APPLICABLE_JFFI: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_ANNOTATION_NOT_APPLICABLE_JFFI", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Inout
    val INOUT_MODIFY_CSTRING_OR_ZEROSIZED: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_INOUT_MODIFY_CSTRING_OR_ZEROSIZED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INOUT_MODIFY_NON_CTYPE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INOUT_MODIFY_NON_CTYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INOUT_MUST_BE_VAR_VARIABLE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INOUT_MUST_BE_VAR_VARIABLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INOUT_MODIFY_HEAP_VARIABLE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INOUT_MODIFY_HEAP_VARIABLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INOUT_MISMATCH: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_INOUT_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_INOUT_ARGUMENT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INVALID_INOUT_ARGUMENT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DUPLICATE_INOUT_ARGUMENT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_DUPLICATE_INOUT_ARGUMENT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // VArrayExtra
    val VARRAY_ARGS_NUMBER_MISMATCH: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_VARRAY_ARGS_NUMBER_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VARRAY_SUBSCRIPT_NUM: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_VARRAY_SUBSCRIPT_NUM", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VARRAY_IN_CFUNC: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_VARRAY_IN_CFUNC", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VARRAY_ARG_TYPE_WITH_REFTYPE: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_VARRAY_ARG_TYPE_WITH_REFTYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // EffectsExtra
    val RESUMPTION_HANDLE_TYPE_ERROR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_RESUMPTION_HANDLE_TYPE_ERROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val RESUMPTION_INCORRECT_RETURN_TYPE: CjDiagnosticFactory2<ConeCangJieType, ConeCangJieType> = CjDiagnosticFactory2("CFIR_RESUMPTION_INCORRECT_RETURN_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMMAND_RESUMPTION_MISMATCH: CjDiagnosticFactory2<ConeCangJieType, ConeCangJieType> = CjDiagnosticFactory2("CFIR_COMMAND_RESUMPTION_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val RESUME_WRONG_RESUMPTION_TYPE: CjDiagnosticFactory1<ConeCangJieType> = CjDiagnosticFactory1("CFIR_RESUME_WRONG_RESUMPTION_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val RETURN_IN_TRY_HANDLE_BLOCK: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_RETURN_IN_TRY_HANDLE_BLOCK", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val USELESS_COMMAND_TYPE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_USELESS_COMMAND_TYPE", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Deprecated
    val DEPRECATED_ERROR: CjDiagnosticFactory4<String, Name, String, String> = CjDiagnosticFactory4("CFIR_DEPRECATED_ERROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATED_WARNING: CjDiagnosticFactory4<String, Name, String, String> = CjDiagnosticFactory4("CFIR_DEPRECATED_WARNING", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATION_WEAKENING: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_DEPRECATION_WEAKENING", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATION_OVERRIDE_ERROR: CjDiagnosticFactory2<String, Name> = CjDiagnosticFactory2("CFIR_DEPRECATION_OVERRIDE_ERROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATION_OVERRIDE_WARNING: CjDiagnosticFactory2<String, Name> = CjDiagnosticFactory2("CFIR_DEPRECATION_OVERRIDE_WARNING", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATION_REDEF_ERROR: CjDiagnosticFactory2<String, Name> = CjDiagnosticFactory2("CFIR_DEPRECATION_REDEF_ERROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEPRECATION_REDEF_WARNING: CjDiagnosticFactory2<String, Name> = CjDiagnosticFactory2("CFIR_DEPRECATION_REDEF_WARNING", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // CommonSpecific
    val COMMON_OPEN_CLASS_NO_INIT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_COMMON_OPEN_CLASS_NO_INIT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MULTIPLE_COMMON_IMPLEMENTATIONS: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_MULTIPLE_COMMON_IMPLEMENTATIONS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS: CjDiagnosticFactory3<Name, String, Name> = CjDiagnosticFactory3("CFIR_COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS: CjDiagnosticFactory2<String, Name> = CjDiagnosticFactory2("CFIR_COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val NOT_MATCHED: CjDiagnosticFactory3<Name, String, String> = CjDiagnosticFactory3("CFIR_NOT_MATCHED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_VAR_NOT_MATCH_LET: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_SPECIFIC_VAR_NOT_MATCH_LET", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_INIT_COMMON_PRIMARY_CONSTRUCTOR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_SPECIFIC_INIT_COMMON_PRIMARY_CONSTRUCTOR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_HAS_DIFFERENT_KIND: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_SPECIFIC_HAS_DIFFERENT_KIND", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_PRIMARY_UNMATCHED_VAR_DECL: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_SPECIFIC_PRIMARY_UNMATCHED_VAR_DECL", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_HAS_DIFFERENT_TYPE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_SPECIFIC_HAS_DIFFERENT_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_HAS_DIFFERENT_MODIFIER: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_SPECIFIC_HAS_DIFFERENT_MODIFIER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_HAS_DIFFERENT_ANNOTATION: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_SPECIFIC_HAS_DIFFERENT_ANNOTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_HAS_DEPRECATED_ANNOTATION: CjDiagnosticFactory3<Name, String, Name> = CjDiagnosticFactory3("CFIR_SPECIFIC_HAS_DEPRECATED_ANNOTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_HAS_DIFFERENT_PARAMETER: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_SPECIFIC_HAS_DIFFERENT_PARAMETER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_HAS_DIFFERENT_SUPER_TYPE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_SPECIFIC_HAS_DIFFERENT_SUPER_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SPECIFIC_HAS_DUPLICATE_EXTENSIONS: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_SPECIFIC_HAS_DUPLICATE_EXTENSIONS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMMON_PACKAGE_HAS_MAIN: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_COMMON_PACKAGE_HAS_MAIN", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMMON_STATIC_LET_CANT_BE_INITIALIZED_IN_STATIC_INIT: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_COMMON_STATIC_LET_CANT_BE_INITIALIZED_IN_STATIC_INIT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER: CjDiagnosticFactory3<Name, String, String> = CjDiagnosticFactory3("CFIR_CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS: CjDiagnosticFactory2<Name, String> = CjDiagnosticFactory2("CFIR_CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMMON_GENERIC_FROZEN_NOT_SUPPORTED: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_COMMON_GENERIC_FROZEN_NOT_SUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMMON_GENERIC_RENAME_NOT_SUPPORTED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_COMMON_GENERIC_RENAME_NOT_SUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // JavaInterop
    val JAVA_INCORRECT_USE_BETWEEN_TYPES: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_INCORRECT_USE_BETWEEN_TYPES", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_NON_JTYPE: CjDiagnosticFactory3<String, String, Name> = CjDiagnosticFactory3("CFIR_JAVA_NON_JTYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_INVALID_UNIT: CjDiagnosticFactory3<String, String, Name> = CjDiagnosticFactory3("CFIR_JAVA_INVALID_UNIT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_APP_INHERIT_EXT: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_JAVA_APP_INHERIT_EXT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_UNSUPPORTED_DECL: CjDiagnosticFactory3<String, String, Name> = CjDiagnosticFactory3("CFIR_JAVA_UNSUPPORTED_DECL", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MISSING_JAVA_INTEROP_ANNOTATION: CjDiagnosticFactory2<String, Name> = CjDiagnosticFactory2("CFIR_MISSING_JAVA_INTEROP_ANNOTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val SHADOW_CANNOT_IN_TYPE_ARGS: CjDiagnosticFactory3<Name, Name, ConeCangJieType> = CjDiagnosticFactory3("CFIR_SHADOW_CANNOT_IN_TYPE_ARGS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val UNSUPPORTED_TYPE_ARGUMENT_IN_JAVA_INTEROP: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_UNSUPPORTED_TYPE_ARGUMENT_IN_JAVA_INTEROP", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val STATIC_MEMBER_IN_INTERFACE_MUST_HAS_BODY: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_STATIC_MEMBER_IN_INTERFACE_MUST_HAS_BODY", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val DEFINE_JAVA_ANNOTATION: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_DEFINE_JAVA_ANNOTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_USE_OF_JAVA_ANNOTATION: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INVALID_USE_OF_JAVA_ANNOTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val INVALID_USE_OF_ANNOTATION_JFFI: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_INVALID_USE_OF_ANNOTATION_JFFI", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val VARIABLE_OF_JAVA_TYPE: CjDiagnosticFactory2<String, ConeCangJieType> = CjDiagnosticFactory2("CFIR_VARIABLE_OF_JAVA_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val GENERIC_PARAMETER_OF_JAVA_TYPE: CjDiagnosticFactory2<Name, ConeCangJieType> = CjDiagnosticFactory2("CFIR_GENERIC_PARAMETER_OF_JAVA_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_INTEROP_NOT_SUPPORTED: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_JAVA_INTEROP_NOT_SUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // JavaMirror
    val JAVA_MIRROR_CTOR_ARG_MUST_BE_JAVA_MIRROR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_MIRROR_CTOR_ARG_MUST_BE_JAVA_MIRROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_MIRROR_METHOD_ARG_MUST_BE_JAVA_MIRROR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_MIRROR_METHOD_ARG_MUST_BE_JAVA_MIRROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_MIRROR_METHOD_RET_UNSUPPORTED: CjDiagnosticFactory2<ConeCangJieType, String> = CjDiagnosticFactory2("CFIR_JAVA_MIRROR_METHOD_RET_UNSUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_MIRROR_PROP_MUST_BE_JAVA_MIRROR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_MIRROR_PROP_MUST_BE_JAVA_MIRROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_MIRROR_SUBTYPE_MUST_BE_ANNOTATED: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_JAVA_MIRROR_SUBTYPE_MUST_BE_ANNOTATED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_MIRROR_CANNOT_INHERIT_PURE_CANGJIE_TYPE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_MIRROR_CANNOT_INHERIT_PURE_CANGJIE_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_IMPL_CANNOT_INHERIT_PURE_CANGJIE_TYPE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_IMPL_CANNOT_INHERIT_PURE_CANGJIE_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_MIRROR_SUBTYPE_ANNO_MUST_INHERIT_MIRROR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_MIRROR_SUBTYPE_ANNO_MUST_INHERIT_MIRROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_MIRROR_CANNOT_BE_EXTENDED_WITH_INTERFACE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_MIRROR_CANNOT_BE_EXTENDED_WITH_INTERFACE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_IMPL_CANNOT_BE_EXTENDED_WITH_INTERFACE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_IMPL_CANNOT_BE_EXTENDED_WITH_INTERFACE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_IMPL_REDEFINITION: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_JAVA_IMPL_REDEFINITION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_HAS_DEFAULT_ANNOTATION_ARGS: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_HAS_DEFAULT_ANNOTATION_ARGS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_HAS_DEFAULT_ANNOTATION_IS_IN_WRONG_PLACE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_HAS_DEFAULT_ANNOTATION_IS_IN_WRONG_PLACE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val JAVA_HAS_DEFAULT_CONFLICT_WITH_STATIC: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_JAVA_HAS_DEFAULT_CONFLICT_WITH_STATIC", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // CJMapping
    val CJMAPPING_STRUCT_GENERIC_NOT_SUPPORTED: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_CJMAPPING_STRUCT_GENERIC_NOT_SUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CJMAPPING_STRUCT_INHERITANCE_INTERFACE_NOT_SUPPORTED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CJMAPPING_STRUCT_INHERITANCE_INTERFACE_NOT_SUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CJMAPPING_DECL_NOT_SUPPORTED: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_CJMAPPING_DECL_NOT_SUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CJMAPPING_METHOD_ARG_NOT_SUPPORTED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_CJMAPPING_METHOD_ARG_NOT_SUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CJMAPPING_METHOD_RET_UNSUPPORTED: CjDiagnosticFactory2<ConeCangJieType, String> = CjDiagnosticFactory2("CFIR_CJMAPPING_METHOD_RET_UNSUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val CJ_MAPPING_GENERIC_METHOD_NOT_GET_INSTANCE_CONFIG: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_CJ_MAPPING_GENERIC_METHOD_NOT_GET_INSTANCE_CONFIG", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // ObjCInterop
    val OBJC_INTEROP_CTOR_PARAM_MUST_BE_OBJC_COMPATIBLE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_INTEROP_CTOR_PARAM_MUST_BE_OBJC_COMPATIBLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_INTEROP_METHOD_PARAM_MUST_BE_OBJC_COMPATIBLE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_INTEROP_METHOD_PARAM_MUST_BE_OBJC_COMPATIBLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_INTEROP_METHOD_RET_MUST_BE_OBJC_COMPATIBLE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_INTEROP_METHOD_RET_MUST_BE_OBJC_COMPATIBLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_INTEROP_PROP_MUST_BE_OBJC_COMPATIBLE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_INTEROP_PROP_MUST_BE_OBJC_COMPATIBLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_INTEROP_FIELD_MUST_BE_OBJC_COMPATIBLE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_INTEROP_FIELD_MUST_BE_OBJC_COMPATIBLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_MIRROR_DECL_CANNOT_INHERIT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_OBJC_MIRROR_DECL_CANNOT_INHERIT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_MIRROR_SUBTYPE_CANNOT_MULTIPLE_INHERIT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_OBJC_MIRROR_SUBTYPE_CANNOT_MULTIPLE_INHERIT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_MIRROR_SUBTYPE_MUST_BE_ANNOTATED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_OBJC_MIRROR_SUBTYPE_MUST_BE_ANNOTATED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_MIRROR_MUST_INHERIT_MIRROR: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_OBJC_MIRROR_MUST_INHERIT_MIRROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_MIRROR_INTEROPLIB_MUST_BE_IMPORTED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_OBJC_MIRROR_INTEROPLIB_MUST_BE_IMPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_INTEROP_NOT_SUPPORTED: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_INTEROP_NOT_SUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_POINTER_ARGUMENT_MUST_BE_OBJC_COMPATIBLE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_OBJC_POINTER_ARGUMENT_MUST_BE_OBJC_COMPATIBLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_INTEROP_TOPLEVEL_PARAM_MUST_BE_OBJC_COMPATIBLE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_INTEROP_TOPLEVEL_PARAM_MUST_BE_OBJC_COMPATIBLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_INTEROP_TOPLEVEL_RET_MUST_BE_OBJC_COMPATIBLE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_INTEROP_TOPLEVEL_RET_MUST_BE_OBJC_COMPATIBLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_METHOD_MUST_HAVE_FOREIGN_NAME: CjDiagnosticFactory2<String, Name> = CjDiagnosticFactory2("CFIR_OBJC_METHOD_MUST_HAVE_FOREIGN_NAME", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_CTOR_MUST_HAVE_FOREIGN_NAME: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_CTOR_MUST_HAVE_FOREIGN_NAME", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_FUNC_ARGUMENT_MUST_BE_OBJC_COMPATIBLE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_FUNC_ARGUMENT_MUST_BE_OBJC_COMPATIBLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_FUNC_CALL_PROPERTY_CAN_ONLY_BE_CALLED: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_FUNC_CALL_PROPERTY_CAN_ONLY_BE_CALLED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_SETTER_NAME_ON_IMMUTABLE_PROP: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_OBJC_SETTER_NAME_ON_IMMUTABLE_PROP", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // ObjCCJMapping
    val OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // ForeignName
    val FOREIGN_NAME_APPEARED_IN_CHILD: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_FOREIGN_NAME_APPEARED_IN_CHILD", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val FOREIGN_NAME_CONFLICTING_ANNOTATION: CjDiagnosticFactory2<Name, Name> = CjDiagnosticFactory2("CFIR_FOREIGN_NAME_CONFLICTING_ANNOTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val FOREIGN_NAME_CONFLICTING_DERIVED_ANNOTATION: CjDiagnosticFactory3<Name, Name, Name> = CjDiagnosticFactory3("CFIR_FOREIGN_NAME_CONFLICTING_DERIVED_ANNOTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // IfAvailable
    val IFAVAILABLE_ARG_NO_NAME: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_IFAVAILABLE_ARG_NO_NAME", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val IFAVAILABLE_ARG_NOT_LITERAL: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_IFAVAILABLE_ARG_NOT_LITERAL", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val IFAVAILABLE_UNKNOWN_ARG_NAME: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_IFAVAILABLE_UNKNOWN_ARG_NAME", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val IFAVAILABLE_LEVEL_LIMIT: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_IFAVAILABLE_LEVEL_LIMIT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // APILevel
    val APILEVEL_MULTI_ANNO: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_APILEVEL_MULTI_ANNO", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val APILEVEL_MISSING_ARG: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_APILEVEL_MISSING_ARG", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val ONLY_LITERAL_SUPPORT: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_ONLY_LITERAL_SUPPORT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val APILEVEL_REF_HIGHER: CjDiagnosticFactory3<Name, Int, Int> = CjDiagnosticFactory3("CFIR_APILEVEL_REF_HIGHER", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val APILEVEL_SYSCAP_WARNING: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_APILEVEL_SYSCAP_WARNING", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val APILEVEL_SYSCAP_ERROR: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_APILEVEL_SYSCAP_ERROR", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val APILEVEL_MULTI_DIFF_SYSCAP: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_APILEVEL_MULTI_DIFF_SYSCAP", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Hide
    val HIDE_MULTI_ANNOTATION: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_HIDE_MULTI_ANNOTATION", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val HIDE_AT_FUNC_PARAM: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_HIDE_AT_FUNC_PARAM", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val HIDE_MISSING_HIDE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_HIDE_MISSING_HIDE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val HIDE_COMPILE_TIME_INVISIBLE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_HIDE_COMPILE_TIME_INVISIBLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val HIDE_DIFF_PARAM: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_HIDE_DIFF_PARAM", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val HIDE_MUST_AT_END: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_HIDE_MUST_AT_END", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Unused
    val UNUSED_IMPORT: CjDiagnosticFactory1<FqName> = CjDiagnosticFactory1("CFIR_UNUSED_IMPORT", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, CjImportItem::class, getRendererFactory())
    val UNUSED_EXPRESSION: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_UNUSED_EXPRESSION", Severity.WARNING, SourceElementPositioningStrategies.DEFAULT, CjExpression::class, getRendererFactory())

    // Mock
    val MOCK_DISABLED: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_MOCK_DISABLED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MOCK_NOT_IN_TEST_MODE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_MOCK_NOT_IN_TEST_MODE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MOCK_UNSUPPORTED_TYPE: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_MOCK_UNSUPPORTED_TYPE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MOCK_WRONG_STATIC_DECL: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_MOCK_WRONG_STATIC_DECL", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MOCK_DOESNT_SUPPORT_MOCKING: CjDiagnosticFactory3<Name, FqName, String> = CjDiagnosticFactory3("CFIR_MOCK_DOESNT_SUPPORT_MOCKING", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MOCK_FROZEN_UNSUPPORTED: CjDiagnosticFactory0 = CjDiagnosticFactory0("CFIR_MOCK_FROZEN_UNSUPPORTED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MOCK_FROZEN_REQUIRED: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_MOCK_FROZEN_REQUIRED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    // Macro
    val MACRO_NOT_EXPANDED: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_MACRO_NOT_EXPANDED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MACRO_EXPANSION_FAILED: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_MACRO_EXPANSION_FAILED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MACRO_SAME_PACKAGE_DEF_CALL: CjDiagnosticFactory2<String, FqName> = CjDiagnosticFactory2("CFIR_MACRO_SAME_PACKAGE_DEF_CALL", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MACRO_ALIAS_CONFLICT: CjDiagnosticFactory2<Name, Collection<FqName>> = CjDiagnosticFactory2("CFIR_MACRO_ALIAS_CONFLICT", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MACRO_EXECUTOR_UNAVAILABLE: CjDiagnosticFactory1<String> = CjDiagnosticFactory1("CFIR_MACRO_EXECUTOR_UNAVAILABLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MACRO_CANNOT_OPEN_LIB: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_MACRO_CANNOT_OPEN_LIB", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MACRO_REEVALUATION_FAILED: CjDiagnosticFactory2<String, String> = CjDiagnosticFactory2("CFIR_MACRO_REEVALUATION_FAILED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MACRO_UNRESOLVED: CjDiagnosticFactory1<Name> = CjDiagnosticFactory1("CFIR_MACRO_UNRESOLVED", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())
    val MACRO_CYCLE: CjDiagnosticFactory2<String, Collection<String>> = CjDiagnosticFactory2("CFIR_MACRO_CYCLE", Severity.ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class, getRendererFactory())

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = CfirErrorsDefaultMessages
}
