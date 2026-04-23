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
        map.put(CfirErrors.IMPORT_CONFLICT, "conflicting imports for name ''{0}''", RENDER_NAME)
        map.put(CfirErrors.IMPORT_ALIAS_CONFLICT, "alias conflict for ''{0}''", RENDER_NAME)
        map.put(CfirErrors.UNRESOLVED_IMPORT, "unresolved import ''{0}''", RENDER_STRING)
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

        // ================================================================
        // General
        // ================================================================
        map.put(CfirErrors.INVALID_NODE_AFTER_CHECK, "semantic error")
        map.put(CfirErrors.UNABLE_TO_INFER_DECL, "unable to infer declaration type, please add type annotation")
        map.put(CfirErrors.MISMATCHED_TYPES_MULTIPLE_ASSIGN, "mismatched types, the expression has type ''{0}''", RENDER_TYPE)
        map.put(CfirErrors.MISMATCHED_TYPES_BECAUSE, "mismatched types: expected ''{0}'', found ''{1}'', expected ''{0}'' because of {2}", RENDER_TYPE, RENDER_TYPE, RENDER_STRING)
        map.put(CfirErrors.AMBIGUOUS_USE, "ambiguous use of ''{0}''", RENDER_NAME)
        map.put(CfirErrors.CONFLICT_WITH_SUB_PACKAGE, "top-level declaration ''{0}'' is conflicted with possible sub-package ''{1}''", RENDER_NAME, RENDER_NAME)
        map.put(CfirErrors.CORE_OBJECT_NOT_FOUND_WHEN_NO_PRELUDE, "class 'Object' of package 'std/core' is not found, cannot use '--no-prelude' option")
        map.put(CfirErrors.ACCESSIBILITY_WITH_MAIN_HINT, "''{0}'' declaration uses {2} types", RENDER_STRING, RENDER_NAME, VISIBILITY)
        map.put(CfirErrors.ACCESSIBILITY_ERROR, "''{0}'' declaration uses {1} types", RENDER_STRING, VISIBILITY)
        map.put(CfirErrors.PARAM_COUNT_MISMATCH, "mismatched number of parameters: expected ''{0}'', found ''{1}''", TO_STRING, TO_STRING)

        // ================================================================
        // Function
        // ================================================================
        map.put(CfirErrors.UNABLE_TO_INFER_RETURN_TYPE, "unable to infer return type, please add type annotation")
        map.put(CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC, "unable to infer generic argument of this function")
        map.put(CfirErrors.INVALID_CALLED_OBJECT, "called object is not a function or constructor")
        map.put(CfirErrors.INVALID_RETURN, "'return' must be used inside a function body")
        map.put(CfirErrors.INVALID_RETURN_IN_STATIC_INIT, "'return' cannot be used inside the static initializer")
        map.put(CfirErrors.INVALID_SUBSCRIPT_ASSIGN_PARAMETER, "overloaded operator '[]' can only have one named parameter 'value'")
        map.put(CfirErrors.INVALID_SUBSCRIPT_ASSIGN_PARAMETER_NUM, "overloaded operator '[]' should have at least one positional parameter for index")
        map.put(CfirErrors.INVALID_SUBSCRIPT_ASSIGN_RETURN, "the return type of subscript assignment must be 'Unit'")
        map.put(CfirErrors.STATIC_FUNCTION_OVERLOAD_CONFLICTS, "overloaded functions ''{0}'' cannot mix static and non-static", RENDER_NAME)
        map.put(CfirErrors.USE_MUTABLE_FUNC_ALONE, "mutable function ''{0}'' cannot be used alone as reference", RENDER_NAME)
        map.put(CfirErrors.UNSAFE_FUNC_CAN_ONLY_BE_CALLED, "the unsafe function can only be called rather than as name reference")
        map.put(CfirErrors.AMBIGUOUS_MATCH_PRIMITIVE_EXTEND, "ambiguous match for function call ''{0}'' of these extended types: {1}", RENDER_NAME, RENDER_NAME_LIST)
        map.put(CfirErrors.CANNOT_HAVE_DEFAULT_PARAM, "optional parameter cannot be used in {0} function", RENDER_STRING)
        map.put(CfirErrors.TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION, "trailing lambda cannot be used for non-function type ''{0}''", RENDER_TYPE)
        map.put(CfirErrors.LAMBDA_MUST_HAVE_TYPE_ANNOTATION, "parameters of this lambda expression must have type annotations")
        map.put(CfirErrors.USE_FUNC_CAPTURE_VAR_ALONE, "{0} capturing mutable variables needs to be called directly", RENDER_STRING)

        // ================================================================
        // Expression
        // ================================================================
        map.put(CfirErrors.UNABLE_TO_INFER_EXPR, "unable to infer the type of this expression, please add type annotation")
        map.put(CfirErrors.EXCEED_FLOAT_LITERAL_RANGE, "the number ''{0}'' exceeds the value range of floating-point literal", RENDER_STRING)
        map.put(CfirErrors.FLOAT_LITERAL_TOO_LARGE, "magnitude of floating-point literal too large for type ''{0}'', maximum is {1}", RENDER_TYPE, RENDER_STRING)
        map.put(CfirErrors.FLOAT_LITERAL_TOO_SMALL, "magnitude of floating-point literal too small for type ''{0}'', minimum is {1}", RENDER_TYPE, RENDER_STRING)
        map.put(CfirErrors.INVALID_UNARY_EXPR, "invalid unary operator ''{0}'' on type ''{1}''", RENDER_STRING, RENDER_TYPE)
        map.put(CfirErrors.INVALID_UNARY_EXPR_WITH_TARGET, "invalid unary operator ''{0}'' on type ''{1}'' with return type ''{2}''", RENDER_STRING, RENDER_TYPE, RENDER_TYPE)
        map.put(CfirErrors.INVALID_SUBSCRIPT_EXPR, "invalid subscript operator [] on type ''{0}'' with index {1}", RENDER_TYPE, RENDER_STRING)
        map.put(CfirErrors.CANNOT_ASSIGN_TO_SUBSCRIPT, "cannot assign to this subscript expression")
        map.put(CfirErrors.NOT_MEMBER_OF, "''{0}'' is not a member of {1} ''{2}''", RENDER_NAME, RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.MEMBER_NOT_IMPORTED, "''{0}'' is not imported", RENDER_NAME)
        map.put(CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE, "cannot assign to immutable value")
        map.put(CfirErrors.UNQUALIFIED_LEFT_VALUE_ASSIGNED, "''{0}'' can not be assigned", RENDER_NAME)
        map.put(CfirErrors.DIFFERENT_OR_PATTERN, "patterns connected by '|' should be of the same kind: {0}", RENDER_STRING)
        map.put(CfirErrors.VAR_IN_OR_PATTERN, "cannot introduce variables in patterns connected by '|'")
        map.put(CfirErrors.VAR_IN_OR_CONDITION, "cannot introduce variables in conditions connected by '||'")
        map.put(CfirErrors.UNREACHABLE_PATTERN, "unreachable pattern")
        map.put(CfirErrors.ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS, "enum constructor ''{0}'' must be used with arguments", RENDER_NAME)
        map.put(CfirErrors.OPTIONAL_CHAIN_NON_OPTIONAL, "cannot use optional chaining on non-optional value of type ''{0}''", RENDER_TYPE)
        map.put(CfirErrors.CAPTURE_BEFORE_INITIALIZATION, "cannot capture variable ''{0}'' before initialization", RENDER_NAME)
        map.put(CfirErrors.INTERPOLATION_IN_CONST_PATTERN, "cannot use string interpolation in constant pattern")
        map.put(CfirErrors.CANNOT_REF_TO_PKG_NAME, "package name cannot be referred independently")
        map.put(CfirErrors.USE_EXPR_WITHOUT_IMPORT, "import ''{0}'' to use the ''{1}'' expression", RENDER_NULLABLE_FQNAME, RENDER_STRING)

        // ================================================================
        // GenericDeep
        // ================================================================
        map.put(CfirErrors.GENERIC_TYPE_INCONSISTENT, "generic types substitutions are inconsistent for ''{0}''", RENDER_NAME)
        map.put(CfirErrors.GENERIC_ARGUMENT_NO_MATCH, "type argument's number does not match type parameter's number")
        map.put(CfirErrors.GENERIC_CONSTRAINT_NOT_LOOSER, "the constraint of type parameter is not looser than parent's constraint")
        map.put(CfirErrors.GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, "generic instantiation ''{0}'' causes ambiguous function ''{1}''", RENDER_NAME, RENDER_NAME)
        map.put(CfirErrors.GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY, "generic parameter ''{0}'' cannot be used in class irrelevant upper bounds ''{1}''", RENDER_NAME, RENDER_TYPE)
        map.put(CfirErrors.GENERIC_PARAM_DIRECTLY_RECURSIVE, "generic parameter ''{0}'' is bounded directly recursively with ''{1}'' which is forbidden", RENDER_NAME, RENDER_NAME)
        map.put(CfirErrors.UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE, "the upper bound ''{0}'' of generic parameter ''{1}'' must be class or interface", RENDER_TYPE, RENDER_NAME)
        map.put(CfirErrors.GENERIC_STATIC_ACCESS, "cannot access static member with generic parameter in '@Java' types")
        map.put(CfirErrors.PRIMITIVE_TYPE_AS_GENERICS_ARG, "only reference types are available for '@Java' generics")
        map.put(CfirErrors.MEET_CONSTRAINT_INDIRECTLY, "types that meet constraints by 'extend' cannot be used in '@Java' generics")
        map.put(CfirErrors.GENERIC_UPPER_BOUNDS_MUST_BE_JAVA_IN_JAVA, "generic type's upper bound in types annotated with '@Java' should be annotated with '@Java' too")

        // ================================================================
        // InheritanceDeep
        // ================================================================
        map.put(CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT, "{0} member ''{1}'' cannot have the same name with {2} member in {3}", RENDER_STRING, RENDER_NAME, RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.INHERIT_SUPER_MEMBER_KIND_INCONSISTENT, "inherited members ''{0}'' have inconsistent decl types", RENDER_NAME)
        map.put(CfirErrors.INHERIT_MEMBER_TYPE_INCONSISTENT, "{0} of the inherited {1} members ''{2}'' are not identical and not in subtype relation", RENDER_STRING, RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC, "abstract class ''{0}'' cannot contain unimplemented static {1} ''{2}''", RENDER_NAME, RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.INVALID_MEMBER_VISIBILITY_IN_CLASS, "the visibility of an ''{0}'' {1} must be 'public' or 'protected'", RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.CANNOT_INHERIT_SEALED, "cannot {0} {1} 'sealed' {2} ''{3}''", RENDER_STRING, RENDER_STRING, RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.INHERIT_THREAD_CONTEXT_INVALID, "user defined decl ''{0}'' not support to inherit, implement or extend 'ThreadContext'", RENDER_NAME)
        map.put(CfirErrors.INHERIT_THREAD_CONTEXT_NOT_OPEN, "''{0}'' cannot be modified with 'open' when inherit, implement or extend 'ThreadContext'", RENDER_NAME)
        map.put(CfirErrors.INHERIT_NOT_RETURN_THIS, "an open function that returns 'This' must keep the return type 'This' when overridden")

        // ================================================================
        // Spawn
        // ================================================================
        map.put(CfirErrors.SPAWN_ARG_INVALID, "invalid argument of spawn expr, user-defined 'ThreadContext' types are prohibited now")
        map.put(CfirErrors.SPAWN_ARG_NO_EFFECT, "argument of spawn expr does not take effect at current backend")

        // ================================================================
        // Interface
        // ================================================================
        map.put(CfirErrors.INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL, "static invocation contains unimplemented static {0} ''{1}''", RENDER_STRING, RENDER_NAME)

        // ================================================================
        // ClassStructSemantics
        // ================================================================
        map.put(CfirErrors.TYPE_UNINITIALIZED_STATIC_FIELD, "the static member variable ''{0}'' is not initialized", RENDER_NAME)
        map.put(CfirErrors.INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER, "instance {0} cannot be used in the finalizer", RENDER_STRING)
        map.put(CfirErrors.NON_ABSTRACT_CLASS_CANNOT_BE_SEALED, "non-abstract class cannot be modified by 'sealed'")
        map.put(CfirErrors.STATIC_VARIABLE_USE_GENERIC_PARAMETER, "static member cannot depend on generic parameter ''{0}''", RENDER_NAME)
        map.put(CfirErrors.CSTRUCT_CANNOT_IMPL_INTERFACES, "struct with @C cannot implement interfaces")
        map.put(CfirErrors.EXPORT_SAME_PRIVATE_DECL, "currently, it is not possible to export two private declarations with the same name")

        // ================================================================
        // ExtendExtra
        // ================================================================
        map.put(CfirErrors.EXTEND_FUNCTION_CANNOT_OVERRIDDEN, "cannot override {0} ''{1}'' in extend of supertype", RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.EXTEND_MEMBER_CANNOT_SHADOW, "extend member ''{0}'' is not allowed to shadow members of ''{1}''", RENDER_NAME, RENDER_NAME)
        map.put(CfirErrors.EXTEND_ILLEGAL_MEMBER, "illegal extend member, only functions, props, associated types are allowed")
        map.put(CfirErrors.EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE, "unable to decide which extension happens first")
        map.put(CfirErrors.EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND, "exported extension cannot indirectly export the functions ''{0}'' of the non-exported extension", RENDER_NAME_LIST)
        map.put(CfirErrors.EXTEND_A_JAVA_TYPE, "types annotated with '@Java' cannot be extended")
        map.put(CfirErrors.EXTEND_REF_TARGET_CANNOT_BE_JAVA_IMPL, "extend declaration ref target cannot be java impl")
        map.put(CfirErrors.TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE, "{0} type ''{1}'' cannot extend imported interface", RENDER_STRING, RENDER_NAME)

        // ================================================================
        // Property
        // ================================================================
        map.put(CfirErrors.PROPERTY_MUST_HAVE_ACCESSORS, "property must have accessors")
        map.put(CfirErrors.IMMUTABLE_PROPERTY_WITH_SETTER, "immutable property cannot have setter")
        map.put(CfirErrors.PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT, "property ''{0}'' should have 'mut' modifier", RENDER_NAME)
        map.put(CfirErrors.PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT, "property ''{0}'' should be immutable", RENDER_NAME)
        map.put(CfirErrors.PROPERTY_MUST_IMPLEMENT_BOTH, "property must implement both getter/setter of interface property ''{0}''", RENDER_NAME)

        // ================================================================
        // ConstDeclaration
        // ================================================================
        map.put(CfirErrors.EXPECT_CONST, "expected 'const' {0}", RENDER_STRING)
        map.put(CfirErrors.CANNOT_DEFINE_VAR_IN_CONST_FUNCTION, "cannot define 'var' variable in 'const' function")
        map.put(CfirErrors.NO_CONST_INIT, "cannot define 'const' member function without 'const' constructor")
        map.put(CfirErrors.CLASS_CONST_INIT_WITH_VAR, "cannot define 'const' constructor with 'var' members in class")

        // ================================================================
        // AnnotationExtra
        // ================================================================
        map.put(CfirErrors.ANNOTATION_ARG_TARGET, "'@Annotation' can only have one named argument 'target'")
        map.put(CfirErrors.ANNOTATION_ARG_TARGET_ARRAY_LIT, "the argument of '@Annotation' should be array literal")
        map.put(CfirErrors.ANNOTATION_NON_PUBLIC, "'@Annotation' modifying non-'public' class is invisible at runtime")
        map.put(CfirErrors.ANNOTATION_CUSTOM_PLACE, "cannot use custom annotation")
        map.put(CfirErrors.ANNOTATION_ERROR_ARG_NUM, "''{0}'' should have {1} arg", RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.ANNOTATION_ERROR_ARG_RANGE, "''{0}'' only supports {1} as arg", RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.ANNOTATION_ERROR_OBJECT, "''{0}'' can only modify {1}", RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.CANNOT_USE_ANNOTATION_JFFI, "cannot use annotation here")
        map.put(CfirErrors.ANNOTATION_NOT_APPLICABLE_JFFI, "'@{0}' not applicable to {1}", RENDER_STRING, RENDER_STRING)

        // ================================================================
        // Inout
        // ================================================================
        map.put(CfirErrors.INOUT_MODIFY_CSTRING_OR_ZEROSIZED, "the expression qualified by 'inout' cannot be of ''{0}''", RENDER_TYPE)
        map.put(CfirErrors.INOUT_MODIFY_NON_CTYPE, "the type of expression qualified by 'inout' must meet 'CType' constraint")
        map.put(CfirErrors.INOUT_MUST_BE_VAR_VARIABLE, "'inout' can only qualify variable defined with 'var'")
        map.put(CfirErrors.INOUT_MODIFY_HEAP_VARIABLE, "the variable qualified by 'inout' cannot be directly or indirectly derived from an instance of a 'class'")
        map.put(CfirErrors.INOUT_CAN_ONLY_USED_IN_CFUNC_CALLING, "'inout' can only be used in a 'CFunc' calling")
        map.put(CfirErrors.INOUT_MISMATCH, "mismatch 'inout' of function argument with type ''{0}''", RENDER_TYPE)
        map.put(CfirErrors.INVALID_INOUT_ARGUMENT, "'inout' argument must be a mutable l-value")
        map.put(CfirErrors.DUPLICATE_INOUT_ARGUMENT, "duplicate 'inout' qualifier on the same argument")

        // ================================================================
        // VArrayExtra
        // ================================================================
        map.put(CfirErrors.VARRAY_ARGS_NUMBER_MISMATCH, "'VArray' constructor accepts only one argument")
        map.put(CfirErrors.VARRAY_SUBSCRIPT_NUM, "'VArray' accepts exactly one subscript index with type of 'Int64'")
        map.put(CfirErrors.VARRAY_IN_CFUNC, "return type of CFunc cannot be 'VArray' type")
        map.put(CfirErrors.VARRAY_ARG_TYPE_WITH_REFTYPE, "''{0}'' directly or indirectly contains an unsupported type", RENDER_TYPE)

        // ================================================================
        // EffectsExtra
        // ================================================================
        map.put(CfirErrors.RESUMPTION_HANDLE_TYPE_ERROR, "the type of the resumption must extend 'effect.Resumption'")
        map.put(CfirErrors.RESUMPTION_INCORRECT_RETURN_TYPE, "the return type of the resumption (''{0}'') does not match the type of the try block (''{1}'')", RENDER_TYPE, RENDER_TYPE)
        map.put(CfirErrors.COMMAND_RESUMPTION_MISMATCH, "the parameter type of the resumption (''{0}'') does not match the result type of the command (''{1}'')", RENDER_TYPE, RENDER_TYPE)
        map.put(CfirErrors.RESUME_WRONG_RESUMPTION_TYPE, "resumptions must be of type 'core.Resumption<T>', but actual type is ''{0}''", RENDER_TYPE)
        map.put(CfirErrors.RETURN_IN_TRY_HANDLE_BLOCK, "Return statements are not allowed within try/handle blocks")
        map.put(CfirErrors.USELESS_COMMAND_TYPE, "useless command type")

        // ================================================================
        // Deprecated
        // ================================================================
        map.put(CfirErrors.DEPRECATED_ERROR, "{0} ''{1}'' is deprecated{2}{3}", RENDER_STRING, RENDER_NAME, RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.DEPRECATED_WARNING, "{0} ''{1}'' is deprecated{2}{3}", RENDER_STRING, RENDER_NAME, RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.DEPRECATION_WEAKENING, "strictness of @Deprecated can not be weaken on inheritors")
        map.put(CfirErrors.DEPRECATION_OVERRIDE_ERROR, "overridden {0} ''{1}'' should be marked with @Deprecated", RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.DEPRECATION_OVERRIDE_WARNING, "overridden {0} ''{1}'' should be marked with @Deprecated", RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.DEPRECATION_REDEF_ERROR, "redefined {0} ''{1}'' should be marked with @Deprecated", RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.DEPRECATION_REDEF_WARNING, "redefined {0} ''{1}'' should be marked with @Deprecated", RENDER_STRING, RENDER_NAME)

        // ================================================================
        // CommonSpecific
        // ================================================================
        map.put(CfirErrors.COMMON_OPEN_CLASS_NO_INIT, "please implement the constructor explicitly for common open class ''{0}''", RENDER_NAME)
        map.put(CfirErrors.MULTIPLE_COMMON_IMPLEMENTATIONS, "'common' {0} has several specific implementations", RENDER_STRING)
        map.put(CfirErrors.COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS, "declaration 'common' extend ''{0}'' has a conflicting private {1} ''{2}''", RENDER_NAME, RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS, "'common' and 'private' modifier conflict on {0} ''{1}'' declaration", RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.NOT_MATCHED, "''{0}'' {1} can not find ''{2}'' match", RENDER_NAME, RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.SPECIFIC_VAR_NOT_MATCH_LET, "'specific' ''{0}'' can not match 'common' ''{1}''", RENDER_NAME, RENDER_NAME)
        map.put(CfirErrors.SPECIFIC_INIT_COMMON_PRIMARY_CONSTRUCTOR, "'specific' init can not be used to implement primary 'common' constructor")
        map.put(CfirErrors.SPECIFIC_HAS_DIFFERENT_KIND, "'specific' decl kind({0}) is not equal to 'common'({1})", RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.SPECIFIC_PRIMARY_UNMATCHED_VAR_DECL, "parameter in 'specific' primary constructor must also be a member variable declaration if it's a member variable declaration in 'common' primary constructor")
        map.put(CfirErrors.COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH, "exhaustive 'common' {0} cannot be matched with non-exhaustive 'specific' {1}", RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.SPECIFIC_HAS_DIFFERENT_TYPE, "'specific' {0} type is not equal to 'common' type", RENDER_STRING)
        map.put(CfirErrors.SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION, "the member {0} must have body in 'specific' {1}", RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.SPECIFIC_HAS_DIFFERENT_MODIFIER, "'specific' {0} modifier is not match 'common' modifier", RENDER_STRING)
        map.put(CfirErrors.SPECIFIC_HAS_DIFFERENT_ANNOTATION, "'specific' {0} annotation is not match 'common' annotation", RENDER_STRING)
        map.put(CfirErrors.SPECIFIC_HAS_DEPRECATED_ANNOTATION, "''{0}'' annotation is not allowed on specific {1} ''{2}''", RENDER_NAME, RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES, "parameter default value should be on either 'common' or 'specific' side, not both")
        map.put(CfirErrors.SPECIFIC_HAS_DIFFERENT_PARAMETER, "'specific' function parameter is not match 'common' parameter")
        map.put(CfirErrors.SPECIFIC_HAS_DIFFERENT_SUPER_TYPE, "'specific' {0} super types is not match 'common' super types", RENDER_STRING)
        map.put(CfirErrors.SPECIFIC_HAS_DUPLICATE_EXTENSIONS, "declaration 'specific' extend ''{0}'' has a conflicting extension", RENDER_NAME)
        map.put(CfirErrors.COMMON_PACKAGE_HAS_MAIN, "main function cannot be used in common package part")
        map.put(CfirErrors.COMMON_STATIC_LET_CANT_BE_INITIALIZED_IN_STATIC_INIT, "'common' static let ''{0}'' can not be initialized in static init", RENDER_NAME)
        map.put(CfirErrors.COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR, "cannot assign to immutable variable ''{0}''", RENDER_NAME)
        map.put(CfirErrors.CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER, "''{0}'' abstract class {1} must have explicit ''{2}'' or 'abstract' modifier", RENDER_NAME, RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY, "abstract {0} can not have body", RENDER_STRING)
        map.put(CfirErrors.EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS, "only common/specific class can have explicitly abstract {0}", RENDER_STRING)
        map.put(CfirErrors.OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON, "open common {0} can not be overridden with abstract specific {1}", RENDER_STRING, RENDER_STRING)
        map.put(CfirErrors.CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS, "specific abstract class ''{0}'' cannot have non-specific abstract {1}", RENDER_NAME, RENDER_STRING)
        map.put(CfirErrors.COMMON_GENERIC_FROZEN_NOT_SUPPORTED, "common/specific declaration {0} with generics cannot be @Frozen", RENDER_STRING)
        map.put(CfirErrors.COMMON_GENERIC_RENAME_NOT_SUPPORTED, "common/specific generic rename is not supported yet")
        map.put(CfirErrors.COMMON_SPECIFIC_ANNOTATION_NOT_ALLOWED, "annotation ''{0}'' is not allowed on a common/specific declaration", RENDER_NAME)

        // ================================================================
        // JavaInterop
        // ================================================================
        map.put(CfirErrors.JAVA_INCORRECT_USE_BETWEEN_TYPES, "type annotated with '@Java[\"ext\"]' can only be used within the declaration which has '@Java[\"ext\"]' annotation")
        map.put(CfirErrors.JAVA_NON_JTYPE, "{0} type in {1} ''{2}'' with '@Java' must meet JType constraint", RENDER_STRING, RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.JAVA_INVALID_UNIT, "{0} type in {1} ''{2}'' with '@Java' can not be 'Unit'", RENDER_STRING, RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.JAVA_APP_INHERIT_EXT, "only types annotated with '@Java[\"ext\"]' can {0} from a type annotated with '@Java[\"ext\"]'", RENDER_STRING)
        map.put(CfirErrors.JAVA_UNSUPPORTED_DECL, "{0} is not supported in {1} ''{2}'' annotated with '@Java'", RENDER_STRING, RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.MISSING_JAVA_INTEROP_ANNOTATION, "{0} ''{1}'' should have '@Java' annotation", RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.SHADOW_CANNOT_IN_TYPE_ARGS, "''{0}'' is not allowed to be used here as type argument, because it shadows field ''{1}'' with its super type ''{2}''", RENDER_NAME, RENDER_NAME, RENDER_TYPE)
        map.put(CfirErrors.UNSUPPORTED_TYPE_ARGUMENT_IN_JAVA_INTEROP, "type argument in java interoperation should meet 'JType' constraint")
        map.put(CfirErrors.STATIC_MEMBER_IN_INTERFACE_MUST_HAS_BODY, "static functions in '@Java'-annotated interfaces must have a body")
        map.put(CfirErrors.DEFINE_JAVA_ANNOTATION, "types annotated with '@Java' cannot be annotated with '@Annotation' together")
        map.put(CfirErrors.INVALID_USE_OF_JAVA_ANNOTATION, "imported Java annotations can only be used with types annotated with '@Java'")
        map.put(CfirErrors.INVALID_USE_OF_ANNOTATION_JFFI, "only imported Java annotations can be used with types annotated with '@Java'")
        map.put(CfirErrors.VARIABLE_OF_JAVA_TYPE, "{0} can not store objects of java interoperability type ''{1}''", RENDER_STRING, RENDER_TYPE)
        map.put(CfirErrors.GENERIC_PARAMETER_OF_JAVA_TYPE, "Can not instantiate generic ''{0}'' with java interoperability type ''{1}''", RENDER_NAME, RENDER_TYPE)
        map.put(CfirErrors.JAVA_INTEROP_NOT_SUPPORTED, "Java interoperability feature ''{0}'' is not yet supported", RENDER_STRING)

        // ================================================================
        // JavaMirror
        // ================================================================
        map.put(CfirErrors.JAVA_MIRROR_CTOR_ARG_MUST_BE_JAVA_MIRROR, "argument type of java-mirrored constructor must be of @JavaMirror type")
        map.put(CfirErrors.JAVA_MIRROR_METHOD_ARG_MUST_BE_JAVA_MIRROR, "argument type of java-mirrored function must be of @JavaMirror type")
        map.put(CfirErrors.JAVA_MIRROR_METHOD_RET_UNSUPPORTED, "return type ''{0}'' of function inside {1} class is not supported", RENDER_TYPE, RENDER_STRING)
        map.put(CfirErrors.JAVA_MIRROR_PROP_MUST_BE_JAVA_MIRROR, "property of java-mirrored declaration must be of @JavaMirror type")
        map.put(CfirErrors.JAVA_MIRROR_SUBTYPE_MUST_BE_ANNOTATED, "super declaration ''{0}'' is inheritable only for declaration annotated with @JavaMirror or @JavaImpl", RENDER_NAME)
        map.put(CfirErrors.JAVA_MIRROR_CANNOT_INHERIT_PURE_CANGJIE_TYPE, "@JavaMirror-annotated declaration cannot inherit pure cangjie type")
        map.put(CfirErrors.JAVA_IMPL_CANNOT_INHERIT_PURE_CANGJIE_TYPE, "@JavaImpl-annotated declaration cannot inherit pure cangjie type")
        map.put(CfirErrors.JAVA_MIRROR_SUBTYPE_ANNO_MUST_INHERIT_MIRROR, "@JavaImpl-annotated declaration must inherit @JavaMirror-annotated declaration")
        map.put(CfirErrors.JAVA_MIRROR_CANNOT_BE_EXTENDED_WITH_INTERFACE, "@JavaMirror class cannot be extended with interface")
        map.put(CfirErrors.JAVA_IMPL_CANNOT_BE_EXTENDED_WITH_INTERFACE, "@JavaImpl class cannot be extended with interface")
        map.put(CfirErrors.JAVA_IMPL_REDEFINITION, "redefinition of java declaration ''{0}''", RENDER_NAME)
        map.put(CfirErrors.JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED, "interoplib.interop must be imported to use java interoperability")
        map.put(CfirErrors.JAVA_HAS_DEFAULT_ANNOTATION_ARGS, "'@JavaHasDefault' can't have arguments")
        map.put(CfirErrors.JAVA_HAS_DEFAULT_ANNOTATION_IS_IN_WRONG_PLACE, "'@JavaHasDefault' can be used only on @JavaMirror interface methods.")
        map.put(CfirErrors.JAVA_HAS_DEFAULT_CONFLICT_WITH_STATIC, "Illegal combination of '@JavaHasDefault' annotation and 'static' modifier.")

        // ================================================================
        // CJMapping
        // ================================================================
        map.put(CfirErrors.CJMAPPING_STRUCT_GENERIC_NOT_SUPPORTED, "cangjie mirror struct type generic {0} is not supported", RENDER_STRING)
        map.put(CfirErrors.CJMAPPING_STRUCT_INHERITANCE_INTERFACE_NOT_SUPPORTED, "cangjie mirror struct type inheritance interface is not supported")
        map.put(CfirErrors.CJMAPPING_DECL_NOT_SUPPORTED, "cangjie mirror decl type is not supported for {0}", RENDER_STRING)
        map.put(CfirErrors.CJMAPPING_METHOD_ARG_NOT_SUPPORTED, "argument type of cangjie mirror decl type member function is not supported")
        map.put(CfirErrors.CJMAPPING_METHOD_RET_UNSUPPORTED, "return type ''{0}'' of function inside {1} type is not supported", RENDER_TYPE, RENDER_STRING)
        map.put(CfirErrors.CJ_MAPPING_GENERIC_METHOD_NOT_GET_INSTANCE_CONFIG, "Instance configuration ''{0}'' has incorrect format.", RENDER_STRING)

        // ================================================================
        // ObjCInterop
        // ================================================================
        map.put(CfirErrors.OBJC_INTEROP_CTOR_PARAM_MUST_BE_OBJC_COMPATIBLE, "param type of {0} constructor must be Objective-C compatible", RENDER_STRING)
        map.put(CfirErrors.OBJC_INTEROP_METHOD_PARAM_MUST_BE_OBJC_COMPATIBLE, "param type of {0} method must be Objective-C compatible", RENDER_STRING)
        map.put(CfirErrors.OBJC_INTEROP_METHOD_RET_MUST_BE_OBJC_COMPATIBLE, "return type of {0} method must be Objective-C compatible", RENDER_STRING)
        map.put(CfirErrors.OBJC_INTEROP_PROP_MUST_BE_OBJC_COMPATIBLE, "{0} property type must be Objective-C compatible", RENDER_STRING)
        map.put(CfirErrors.OBJC_INTEROP_FIELD_MUST_BE_OBJC_COMPATIBLE, "{0} field type must be Objective-C compatible", RENDER_STRING)
        map.put(CfirErrors.OBJC_MIRROR_DECL_CANNOT_INHERIT, "Objective-C mirror cannot inherit other supertypes")
        map.put(CfirErrors.OBJC_MIRROR_SUBTYPE_CANNOT_MULTIPLE_INHERIT, "Objective-C mirror subtype cannot inherit multiple types (only 1 interface or 1 class is allowed)")
        map.put(CfirErrors.OBJC_MIRROR_SUBTYPE_MUST_BE_ANNOTATED, "Objective-C mirror subtype must be annotated with @ObjCMirror or @ObjCImpl")
        map.put(CfirErrors.OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR, "@ObjCImpl declaration must inherit @ObjCMirror")
        map.put(CfirErrors.OBJC_MIRROR_MUST_INHERIT_MIRROR, "@ObjCMirror declaration cannot inherit not @ObjCMirror declarations")
        map.put(CfirErrors.OBJC_MIRROR_INTEROPLIB_MUST_BE_IMPORTED, "interoplib.objc must be imported to use Objective-C interoperability")
        map.put(CfirErrors.OBJC_INTEROP_NOT_SUPPORTED, "Objective-C interoperability feature ''{0}'' is not yet supported", RENDER_STRING)
        map.put(CfirErrors.OBJC_POINTER_ARGUMENT_MUST_BE_OBJC_COMPATIBLE, "ObjCPointer can only be used with Objective-C compatible types")
        map.put(CfirErrors.OBJC_INTEROP_TOPLEVEL_PARAM_MUST_BE_OBJC_COMPATIBLE, "param type of Objective-C mirror top-level function ''{0}'' must be Objective-C compatible", RENDER_STRING)
        map.put(CfirErrors.OBJC_INTEROP_TOPLEVEL_RET_MUST_BE_OBJC_COMPATIBLE, "return type of Objective-C mirror top-level function ''{0}'' must be Objective-C compatible", RENDER_STRING)
        map.put(CfirErrors.OBJC_METHOD_MUST_HAVE_FOREIGN_NAME, "{0} declaration method ''{1}'' with more than one parameter must have @ForeignName annotation", RENDER_STRING, RENDER_NAME)
        map.put(CfirErrors.OBJC_CTOR_MUST_HAVE_FOREIGN_NAME, "{0} declaration constructor with more than one parameter must have @ForeignName annotation", RENDER_STRING)
        map.put(CfirErrors.OBJC_FUNC_ARGUMENT_MUST_BE_OBJC_COMPATIBLE, "{0} can only be used with function type over Objective-C compatible types", RENDER_STRING)
        map.put(CfirErrors.OBJC_FUNC_CALL_PROPERTY_CAN_ONLY_BE_CALLED, "{0} property 'call' can only be called directly, no other operations are permitted", RENDER_STRING)
        map.put(CfirErrors.OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS, "@ObjCImpl class must have @ObjCMirror super class")
        map.put(CfirErrors.OBJC_SETTER_NAME_ON_IMMUTABLE_PROP, "@ForeignSetterName cannot be specified on immutable property")

        // ================================================================
        // ObjCCJMapping
        // ================================================================
        map.put(CfirErrors.OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED, "cangjie mirror decl type inheritance interface is not supported")
        map.put(CfirErrors.OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED, "cangjie mirror decl type generic {0} is not supported", RENDER_STRING)

        // ================================================================
        // ForeignName
        // ================================================================
        map.put(CfirErrors.FOREIGN_NAME_APPEARED_IN_CHILD, "@{0} could not appear on overridden declaration", RENDER_NAME)
        map.put(CfirErrors.FOREIGN_NAME_CONFLICTING_ANNOTATION, "Declaration ''{0}'' has a conflicting @{1} annotation", RENDER_NAME, RENDER_NAME)
        map.put(CfirErrors.FOREIGN_NAME_CONFLICTING_DERIVED_ANNOTATION, "Declaration ''{0}'' has a conflicting derived @{1} ''{2}''", RENDER_NAME, RENDER_NAME, RENDER_NAME)

        // ================================================================
        // IfAvailable
        // ================================================================
        map.put(CfirErrors.IFAVAILABLE_ARG_NO_NAME, "the first argument of @IfAvailable expression must have a name")
        map.put(CfirErrors.IFAVAILABLE_ARG_NOT_LITERAL, "the first argument of @IfAvailable expression must be a literal expression")
        map.put(CfirErrors.IFAVAILABLE_UNKNOWN_ARG_NAME, "unknown parameter name ''{0}''", RENDER_STRING)
        map.put(CfirErrors.IFAVAILABLE_LEVEL_LIMIT, "'@IfAvailable' feature is not available in device where the APILevel is less than 19 due to missing capability in ROM")

        // ================================================================
        // APILevel
        // ================================================================
        map.put(CfirErrors.APILEVEL_MULTI_ANNO, "annotate more than one '@!APILevel'")
        map.put(CfirErrors.APILEVEL_MISSING_ARG, "annotation missing named argument ''{0}'' or unable to read as numerical value", RENDER_NAME)
        map.put(CfirErrors.ONLY_LITERAL_SUPPORT, "only {0} literal values are supported for now", RENDER_STRING)
        map.put(CfirErrors.APILEVEL_REF_HIGHER, "cannot reference ''{0}''(level: {1}) which higher than level of the current scope(level: {2})", RENDER_NAME, TO_STRING, TO_STRING)
        map.put(CfirErrors.APILEVEL_SYSCAP_WARNING, "inappropriate syscap ''{0}''", RENDER_NAME)
        map.put(CfirErrors.APILEVEL_SYSCAP_ERROR, "inappropriate syscap ''{0}''", RENDER_NAME)
        map.put(CfirErrors.APILEVEL_MULTI_DIFF_SYSCAP, "declaration mark with different syscap")

        // ================================================================
        // Hide
        // ================================================================
        map.put(CfirErrors.HIDE_MULTI_ANNOTATION, "cannot be annotated with '@!Hide' more than once")
        map.put(CfirErrors.HIDE_AT_FUNC_PARAM, "function parameter cannot be annotated with '@!Hide'")
        map.put(CfirErrors.HIDE_MISSING_HIDE, "should be marked with '@!Hide' to be hidden")
        map.put(CfirErrors.HIDE_COMPILE_TIME_INVISIBLE, "'Hide' annotation must be visible at compile time")
        map.put(CfirErrors.HIDE_DIFF_PARAM, "the parameter 'isChecked' of '@!Hide' is {0}", RENDER_STRING)
        map.put(CfirErrors.HIDE_MUST_AT_END, "annotation ''{0}'' must be placed below all macros and annotations", RENDER_STRING)

        // ================================================================
        // Unused
        // ================================================================
        map.put(CfirErrors.UNUSED_IMPORT, "unused import ''{0}''", RENDER_NULLABLE_FQNAME)

        // ================================================================
        // Mock
        // ================================================================
        map.put(CfirErrors.MOCK_DISABLED, "mocking features are disabled, you can enable them by passing {0} compilation option explicitly, or using default mode", RENDER_STRING)
        map.put(CfirErrors.MOCK_NOT_IN_TEST_MODE, "mocking features can be used only in the test mode, please pass {0} compilation option to compile the package in the test mode", RENDER_STRING)
        map.put(CfirErrors.MOCK_UNSUPPORTED_TYPE, "only mocking of classes or interfaces is supported")
        map.put(CfirErrors.MOCK_WRONG_STATIC_DECL, "static/top-level declaration to mock shouldn't be private, local, constant or constructor")
        map.put(CfirErrors.MOCK_DOESNT_SUPPORT_MOCKING, "''{0}'' doesn''t support mocking, please be sure that its package ''{1}'' is mock-compatible (was compiled with {2} compilation option)", RENDER_NAME, RENDER_NULLABLE_FQNAME, RENDER_STRING)
        map.put(CfirErrors.MOCK_FROZEN_UNSUPPORTED, "mocking of frozen declarations (marked with @Frozen annotation) are not supported")
        map.put(CfirErrors.MOCK_FROZEN_REQUIRED, "generic wrapper function ''{0}'' for createMock/createSpy calls should be marked with @Frozen annotation", RENDER_NAME)

    }
}
