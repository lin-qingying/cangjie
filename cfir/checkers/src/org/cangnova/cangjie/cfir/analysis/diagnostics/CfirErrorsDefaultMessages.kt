package org.cangnova.cangjie.cfir.analysis.diagnostics

import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.ARGUMENT_TYPE_MISMATCH
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.ASSIGNMENT_TYPE_MISMATCH
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.CANNOT_INFER_PARAMETER_TYPE
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.CONST_EVAL_ARITHMETIC_OVERFLOW
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.CONST_EVAL_DIVIDE_BY_ZERO
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.CONST_EVAL_NEGATIVE_SHIFT_COUNT
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.CONST_EVAL_SHIFT_COUNT_OVERFLOW
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.LITERAL_NUMERIC_OVERFLOW
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.NEW_INFERENCE_ERROR
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.NO_CONSTRUCTOR
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.PATTERN_INITIALIZER_TYPE_MISMATCH
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.REDECLARATION
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.INVALID_OPERATOR_PARAMETER_COUNT
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.RETURN_TYPE_MISMATCH
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.TYPE_MISMATCH
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.CLASSIFIER_REDECLARATION
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.CONFLICTING_OVERLOADS
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticRenderers.DECLARATION_NAME
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticRenderers.RENDER_TYPE
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticRenderers.RENDER_TYPE_LIST
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactoryToRendererMap
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRenderers.NOT_RENDERED
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRenderers.NULLABLE_STRING
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRenderers.TO_STRING
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRenderers.VISIBILITY
import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_NAME
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_NAME_LIST
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_NULLABLE_FQNAME
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_NULLABLE_NAME
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_STRING
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_STRING_LIST

object CfirErrorsDefaultMessages : BaseDiagnosticRendererFactory() {

    override val MAP: CjDiagnosticFactoryToRendererMap by CjDiagnosticFactoryToRendererMap("FIR") { map ->
        map.put(NO_CONSTRUCTOR, "No constructor available for this type.")
        map.put(
            CfirErrors.ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR,
            "Enum type ''{0}'' cannot be used as a type constructor; use an enum constructor instead.",
            RENDER_NAME,
        )
        map.put(CONFLICTING_OVERLOADS, "Conflicting overloads: {0}", RENDER_STRING_LIST)
        map.put(REDECLARATION, "Conflicting declarations: {0}", RENDER_STRING_LIST)
        map.put(CLASSIFIER_REDECLARATION, "Redeclaration: {0}", RENDER_STRING_LIST)
        map.put(
            CfirErrors. UNRESOLVED_IMPORT,
            "Unresolved reference ''{0}''.",
            NULLABLE_STRING,
        )
        map.put(CfirErrors.IMPORT_CONFLICT, "conflicting imports for name ''{0}''", RENDER_NAME)
        map.put(CfirErrors.IMPORT_ALIAS_CONFLICT, "alias conflict for ''{0}''", RENDER_NAME)
        map.put(CfirErrors.SUPER_TYPES_SELF_REFERENCE, "type ''{0}'' cannot inherit from itself", RENDER_NAME)
        map.put(CfirErrors.SUPER_TYPES_DUPLICATE, "duplicate super type ''{0}''", RENDER_NAME)
        map.put(CfirErrors.ILLEGAL_EXTENDED_TYPE, "illegal extended type ''{0}''", RENDER_NAME)
        map.put(CfirErrors.EXTEND_DUPLICATE_INTERFACE, "duplicate extend interface ''{0}''", RENDER_NAME)
        map.put(CfirErrors.EXTEND_NOT_INTERFACE, "inherited type ''{0}'' in extend declaration is not an interface", RENDER_NAME)
        map.put(
            CfirErrors.EXTEND_INTERFACE_NOT_EXTENDABLE,
            "interface ''{0}'' cannot be used in an extend declaration",
            RENDER_NAME,
        )
        map.put(CfirErrors.EXTEND_ORPHAN_RULE, "extend declaration violates orphan rule for target ''{0}''", RENDER_NAME)
        map.put(CfirErrors.EXTEND_GENERIC_USAGE, "extend type parameter ''{0}'' is unused in extend signatures", RENDER_NAME)
        map.put(
            CfirErrors.EXTEND_IMMUTABLE_MUT_INTERFACE,
            "immutable extend cannot implement interface ''{0}'' because it contains mut property ''{1}''",
            RENDER_NAME,
            RENDER_NAME,
        )
        map.put(
            CfirErrors.EXTEND_IMMUTABLE_MUT_PROPERTY,
            "immutable extend cannot declare mut property ''{0}''",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.EXTEND_IMMUTABLE_INDEX_ASSIGNMENT,
            "immutable extend cannot declare index assignment operator ''{0}''",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.EXTEND_C_TYPE_NOT_ALLOWED,
            "foreign interop type ''{0}'' cannot be the target of an extend declaration",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.EXTEND_SUPER_NOT_ALLOWED,
            "'super' is not allowed inside extend declarations",
        )
        map.put(
            CfirErrors.STRUCT_SUPER_NOT_ALLOWED,
            "'super' is not allowed inside struct declarations",
        )
        map.put(
            CfirErrors.ENUM_SUPER_NOT_ALLOWED,
            "'super' is not allowed inside enum declarations",
        )
        map.put(
            CfirErrors.INTERFACE_SUPER_NOT_ALLOWED,
            "'super' is not allowed inside interface declarations",
        )
        map.put(CfirErrors.EXTEND_SPECIALIZATION_CONFLICT, "specialization conflict detected for interface ''{0}''", RENDER_NAME)
        map.put(
            CfirErrors.EXTEND_DEFAULT_IMPLEMENTATION_CONFLICT,
            "default member ''{0}'' from interface ''{1}'' conflicts across extend declarations",
            RENDER_NAME,
            RENDER_NAME,
        )
        map.put(CfirErrors.INTERFACE_CANNOT_INHERIT_CLASS, "interface ''{0}'' cannot inherit non-interface type ''{1}''", RENDER_NAME, RENDER_NAME)
        map.put(CfirErrors.MULTIPLE_CLASS_SUPER_TYPES, "type ''{0}'' has multiple class supertypes: {1}", RENDER_NAME, RENDER_NAME_LIST)
        map.put(CfirErrors.STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE, "declaration ''{0}'': static declaration cannot be open/abstract/override", RENDER_NULLABLE_NAME)
        map.put(
            CfirErrors.MUT_ONLY_ON_FUNCTION,
            "declaration ''{0}'': mut modifier is only valid on property declarations and function declarations inside struct bodies",
            RENDER_NULLABLE_NAME,
        )
        map.put(CfirErrors.NOTHING_TO_OVERRIDE, "Nothing to override.")
        map.put(CfirErrors.OVERRIDE_STATIC_ERROR, "'override' cannot be used to modify a static ''{0}''.", RENDER_STRING)
        map.put(CfirErrors.REDEF_INSTANCE_ERROR, "'redef' cannot be used to modify an instance ''{0}''.", RENDER_STRING)
        map.put(
            CfirErrors.INVISIBLE_MEMBER,
            "Cannot access member ''{0}'': it is ''{1}'' in this context.",
            RENDER_STRING,
            RENDER_STRING,
        )
        map.put(
            CfirErrors.INVISIBLE_REFERENCE,
            "Cannot access reference ''{0}'': it is ''{1}'' in this context.",
            RENDER_STRING,
            RENDER_STRING,
        )
        map.put(
            CfirErrors.OVERRIDING_RETURN_TYPE_MISMATCH,
            "Return type of overriding declaration for ''{2}'' is ''{0}'', but ''{1}'' was expected.",
            RENDER_TYPE,
            RENDER_TYPE,
            RENDER_NAME,
        )
        map.put(
            CfirErrors.CANNOT_OVERRIDE_INVISIBLE_MEMBER,
            "Cannot override invisible member ''{0}''.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.PARAM_NAMED_MISMATCHED,
            "Parameter naming of this declaration does not match overridden member ''{0}''.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.CLASS_NOT_OPEN_FOR_INHERITANCE,
            "Class ''{0}'' is not open for inheritance.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.NO_VALUE_FOR_PARAMETER,
            "No value passed for parameter ''{0}''.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.TOO_MANY_ARGUMENTS,
            "Too many arguments for ''{0}''.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.NAMED_PARAMETER_NOT_FOUND,
            "No parameter named ''{0}''.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.ARGUMENT_PASSED_TWICE,
            "Argument already passed for this parameter.",
        )
        map.put(
            CfirErrors.NAMED_ARGUMENTS_NOT_ALLOWED,
            "Named arguments are not allowed for {0}.",
            RENDER_STRING,
        )
        map.put(
            CfirErrors.MIXING_NAMED_AND_POSITIONAL_ARGUMENTS,
            "Positional argument cannot appear after named argument.",
        )
        map.put(
            CfirErrors.NEED_NAMED_ARGUMENT,
            "Missing argument prefix for named parameter ''{0}''.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.AMBIGUOUS_CONSTRUCTOR_CALL,
            "Ambiguous constructor call for ''{0}''.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.AMBIGUOUS_FUNCTION_CALL,
            "Ambiguous function call for ''{0}''.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.RECURSIVE_CONSTRUCTOR_CALL,
            "Recursive constructor call detected.",
        )
        map.put(
            CfirErrors.ILLEGAL_THIS_OR_SUPER_CALL,
            "Illegal place of calling ''{0}''.",
            RENDER_STRING,
        )
        map.put(
            CfirErrors.EXPLICIT_SUPER_CALL_REQUIRED,
            "Explicit super constructor call is required.",
        )
        map.put(
            CfirErrors.INVALID_LOOP_CONTROL,
            "'break' or 'continue' must be used inside a loop.",
        )
        map.put(
            CfirErrors.USED_BEFORE_INITIALIZATION,
            "Variable ''{0}'' is used before initialization.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.CLASS_UNINITIALIZED_FIELD,
            "Member variable ''{0}'' is not initialized in this constructor.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS,
            "''{0}'' is not found for generic type ''{1}'' in its upper bounds.",
            RENDER_NAME,
            RENDER_NAME,
        )
        map.put(
            CfirErrors.GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS,
            "Method ''{0}'' is not found for generic type ''{1}'' in its upper bounds.",
            RENDER_NAME,
            RENDER_NAME,
        )
        map.put(
            CfirErrors.CANNOT_MODIFY_VAR,
            "Instance member variable ''{0}'' cannot be modified in immutable function.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION,
            "Immutable function ''{0}'' cannot access mutable function ''{1}''.",
            RENDER_NAME,
            RENDER_NAME,
        )
        map.put(
            CfirErrors.ANNOTATION_NO_CONST_INIT,
            "class with '@Annotation' should have 'const' constructor.",
        )
        map.put(
            CfirErrors.INVALID_CFUNC_RETURN_TYPE,
            "foreign function return type must satisfy CType, but ''{0}'' was found.",
            RENDER_TYPE,
        )
        map.put(
            CfirErrors.EFFECTS_FEATURE_DISABLED,
            "effects feature is disabled for ''{0}''.",
            RENDER_STRING,
        )
        map.put(
            CfirErrors.COMMAND_INCOMPATIBLE_TYPE,
            "the performed expression must implement ''stdx.effect.Command<T>'', but actual type is ''{0}''.",
            RENDER_TYPE,
        )
        map.put(
            CfirErrors.COMMAND_HANDLE_TYPE_ERROR,
            "the command handle type must implement ''stdx.effect.Command<T>'', but actual type is ''{0}''.",
            RENDER_TYPE,
        )
        map.put(
            CfirErrors.IMPLICIT_RESUME_OUTSIDE_HANDLER,
            "''resume'' outside of an immediate handler must have a resumption argument.",
        )
        map.put(
            CfirErrors.RESUME_NO_WITH,
            "a resumption of non-Unit type ''{0}'' must have a ''with'' or ''throwing'' clause.",
            RENDER_TYPE,
        )
        map.put(
            CfirErrors.RESUME_THROWING_MISMATCH_TYPE,
            "the type of ''resume throwing'' must be a subtype of ''std.core.Exception'' or ''std.core.Error'', but actual type is ''{0}''.",
            RENDER_TYPE,
        )
        map.put(
            CfirErrors.MISMATCHING_HANDLE_BLOCK,
            "The type of this handle block is ''{0}'', which mismatches the smallest common supertype ''{1}'' of previous branches.",
            RENDER_TYPE,
            RENDER_TYPE,
        )
        map.put(
            INVALID_OPERATOR_PARAMETER_COUNT,
            "invalid number of parameters for operator ''{0}'': expected {1}, but found {2}",
            RENDER_STRING,
            RENDER_STRING,
            RENDER_STRING,
        )
        map.put(CfirErrors.REPEATED_MODIFIER, "Repeated modifier ''{0}''.", TO_STRING)
        map.put(CfirErrors.REDUNDANT_MODIFIER, "Modifier ''{0}'' is redundant because ''{1}'' is present.", TO_STRING, TO_STRING)
        map.put(CfirErrors.INCOMPATIBLE_MODIFIERS, "Incompatible modifiers: ''{0}'' and ''{1}''.", TO_STRING, TO_STRING)
        map.put(CfirErrors.WRONG_MODIFIER_TARGET, "Modifier ''{0}'' is not applicable to ''{1}''.", TO_STRING, RENDER_STRING)
        map.put(
            CfirErrors.WRONG_MODIFIER_CONTAINING_DECLARATION,
            "Modifier ''{0}'' is not applicable inside ''{1}''.",
            TO_STRING,
            RENDER_STRING,
        )
        map.put(CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET, "Modifier ''{0}'' is redundant for ''{1}''.", TO_STRING, RENDER_STRING)
        map.put(CfirErrors.DEPRECATED_MODIFIER_FOR_TARGET, "Modifier ''{0}'' is deprecated for ''{1}''.", TO_STRING, RENDER_STRING)
        map.put(
            CfirErrors.DEPRECATED_MODIFIER_CONTAINING_DECLARATION,
            "Modifier ''{0}'' is deprecated inside ''{1}''.",
            TO_STRING,
            RENDER_STRING,
        )
        map.put(CfirErrors.DEPRECATED_MODIFIER_PAIR, "Modifier ''{0}'' is deprecated in combination with ''{1}''.", TO_STRING, TO_STRING)
        map.put(
            CfirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE,
            "a deriving member must be at least as visible as its base member. the visibility of the base ''{0}'' is ''{1}''",
            RENDER_NAME,
            VISIBILITY,
        )
        map.put(CfirErrors.NON_EXHAUSTIVE_MATCH, "match expression is not exhaustive. Missing cases: {0}", RENDER_STRING_LIST)
        map.put(
            CfirErrors.TUPLE_PATTERN_NOT_MATCH,
            "{0} isn't a tuple to match tuple pattern.",
            RENDER_STRING,
        )
        map.put(
            CfirErrors.PATTERN_NOT_MATCH,
            "{0} pattern is not matched.",
            RENDER_STRING,
        )
        map.put(
            CfirErrors.ENUM_PATTERN_PARAM_SIZE_ERROR,
            "enum pattern's parameters size is wrong.",
        )
        map.put(
            CfirErrors.NOT_OVERLOAD_IN_MATCH,
            "No overloaded '==' function in match case pattern.",
        )
        map.put(
            CfirErrors.MATCH_CASE_HAS_NO_TYPE,
            "This match case has no type.",
        )
        map.put(
            CfirErrors.UNRESOLVED_REFERENCE,
            "Unresolved reference: ''{0}''.",
            RENDER_STRING,
            NOT_RENDERED,
        )
        map.put(
            CfirErrors.INVALID_BINARY_OPERATOR,
            "invalid binary operator ''{0}'' on type ''{1}'' and ''{2}'', you may want to implement ''operator func {0}(right: {2})'' for type ''{1}''",
            RENDER_STRING,
            RENDER_STRING,
            RENDER_STRING,
        )
        map.put(
            CfirErrors.NO_MATCHING_OPERATOR_INVOKE,
            "no matching function for operator ''()'' on type ''{1}''",
            RENDER_STRING,
            RENDER_TYPE,
        )
        map.put(
            LITERAL_NUMERIC_OVERFLOW,
            "Numeric literal ''{0}'' is out of range for target type ''{1}''.",
            RENDER_STRING,
            RENDER_TYPE,
        )
        map.put(
            CONST_EVAL_DIVIDE_BY_ZERO,
            "Constant evaluation failed: operator ''{0}'' has a zero divisor.",
            RENDER_STRING,
        )
        map.put(
            CONST_EVAL_ARITHMETIC_OVERFLOW,
            "Constant evaluation overflow in operator ''{0}''.",
            RENDER_STRING,
        )
        map.put(
            CONST_EVAL_NEGATIVE_SHIFT_COUNT,
            "Shift count is negative during constant evaluation.",
        )
        map.put(
            CONST_EVAL_SHIFT_COUNT_OVERFLOW,
            "Shift count overflow during constant evaluation.",
        )
        map.put(
            TYPE_MISMATCH,
            "Type mismatch: inferred type is ''{1}'', but ''{0}'' was expected.",
            RENDER_TYPE,
            RENDER_TYPE,
            NOT_RENDERED,
        )
        map.put(
            PATTERN_INITIALIZER_TYPE_MISMATCH,
            "Initializer type mismatch: inferred type is ''{1}'', but ''{0}'' was expected for pattern variable.",
            RENDER_TYPE,
            RENDER_TYPE,
            NOT_RENDERED,
        )
        map.put(
            ARGUMENT_TYPE_MISMATCH,
            "Argument type mismatch: actual type is ''{1}'', but ''{0}'' was expected.",
            RENDER_TYPE,
            RENDER_TYPE,
            NOT_RENDERED,
        )
        map.put(
            RETURN_TYPE_MISMATCH,
            "Return type mismatch: expected ''{0}'', actual ''{1}''.",
            RENDER_TYPE,
            RENDER_TYPE,
            NOT_RENDERED,
        )
        map.put(
            CfirErrors.ASSIGNMENT_TYPE_MISMATCH,
            "Assignment type mismatch: expected ''{0}'', actual ''{1}''.",
            RENDER_TYPE,
            RENDER_TYPE,
            NOT_RENDERED,
        )
        map.put(
            CfirErrors.VARRAY_SIZE_MISMATCH,
            "VArray size mismatch: expected size ''{0}'', actual size ''{1}'' for element type ''{2}''.",
            TO_STRING,
            TO_STRING,
            RENDER_TYPE,
        )
        map.put(
            CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT,
            "generic type ''{0}'' should be used with type argument",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED,
            "class ''{0}'' does not implement inherited abstract members",
            RENDER_NAME,
        )

        map.put(CANNOT_INFER_PARAMETER_TYPE, "Cannot infer type for type parameter ''{0}''. Specify it explicitly.", DECLARATION_NAME)
        map.put(NEW_INFERENCE_ERROR, "Inference error: {0}", RENDER_STRING)
        map.put(
            TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR,
            "Cannot infer type parameter ''{0}'' only from input positions.",
            DECLARATION_NAME,
        )
        map.put(
            BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION,
            "Builder inference cannot infer type parameter ''{0}'' for declaration ''{1}'' across multiple lambda arguments.",
            RENDER_NAME,
            RENDER_NAME,
        )
        map.put(
            CfirErrors.NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER,
            "''{0}'' is not a type parameter of this declaration.",
            RENDER_NAME,
        )
        map.put(
            CfirErrors.ONLY_ONE_CLASS_BOUND_ALLOWED,
            "Only one upper bound can be a concrete class, struct, enum, or primitive type.",
        )
        map.put(
            CfirErrors.REPEATED_BOUND,
            "Type parameter already has this bound.",
        )
        map.put(
            CfirErrors.CONFLICTING_UPPER_BOUNDS,
            "Type parameter has conflicting upper bounds.",
        )
        map.put(
            INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION,
            "Type parameter ''{0}'' was inferred into an empty intersection of {1}. Reason: {2}{3}",
            RENDER_STRING,
            RENDER_TYPE_LIST,
            RENDER_STRING,
            RENDER_STRING,
        )
        map.put(
            INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION,
            "Type parameter ''{0}'' was inferred into a possibly empty intersection of {1}. Reason: {2}{3}",
            RENDER_STRING,
            RENDER_TYPE_LIST,
            RENDER_STRING,
            RENDER_STRING,
        )


    }
}
