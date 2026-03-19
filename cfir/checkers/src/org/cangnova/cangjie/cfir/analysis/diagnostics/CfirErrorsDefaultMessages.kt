package org.cangnova.cangjie.cfir.analysis.diagnostics

import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.ARGUMENT_TYPE_MISMATCH
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.ASSIGNMENT_TYPE_MISMATCH
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.CONST_EVAL_ARITHMETIC_OVERFLOW
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.CONST_EVAL_DIVIDE_BY_ZERO
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.LITERAL_NUMERIC_OVERFLOW
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.PATTERN_INITIALIZER_TYPE_MISMATCH
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.RETURN_TYPE_MISMATCH
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors.TYPE_MISMATCH
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticRenderers.RENDER_TYPE
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactoryToRendererMap
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRenderers.NOT_RENDERED
import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_NAME
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_NAME_LIST
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_NULLABLE_FQNAME
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_NULLABLE_NAME
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_STRING
import org.cangnova.cangjie.cfir.diagnostics.rendering.CjDiagnosticRenderers.RENDER_STRING_LIST

object CfirErrorsDefaultMessages : BaseDiagnosticRendererFactory() {

    override val MAP: CjDiagnosticFactoryToRendererMap by CjDiagnosticFactoryToRendererMap("FIR") { map ->
        map.put(CfirErrors.IMPORT_TARGET_NOT_FOUND, "unresolved import target ''{0}''", RENDER_NULLABLE_FQNAME)
        map.put(CfirErrors.IMPORT_CONFLICT, "conflicting imports for name ''{0}''", RENDER_NAME)
        map.put(CfirErrors.IMPORT_ALIAS_CONFLICT, "alias conflict for ''{0}''", RENDER_NAME)
        map.put(CfirErrors.SUPER_TYPES_SELF_REFERENCE, "type ''{0}'' cannot inherit from itself", RENDER_NAME)
        map.put(CfirErrors.SUPER_TYPES_DUPLICATE, "duplicate super type ''{0}''", RENDER_NAME)
        map.put(CfirErrors.ILLEGAL_EXTENDED_TYPE, "illegal extended type ''{0}''", RENDER_NAME)
        map.put(CfirErrors.EXTEND_DUPLICATE_INTERFACE, "duplicate extend interface ''{0}''", RENDER_NAME)
        map.put(CfirErrors.EXTEND_NOT_INTERFACE, "inherited type ''{0}'' in extend declaration is not an interface", RENDER_NAME)
        map.put(CfirErrors.EXTEND_ORPHAN_RULE, "extend declaration violates orphan rule for target ''{0}''", RENDER_NAME)
        map.put(CfirErrors.EXTEND_GENERIC_USAGE, "extend type parameter ''{0}'' is unused in extend signatures", RENDER_NAME)
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
        map.put(CfirErrors.MUT_ONLY_ON_FUNCTION, "declaration ''{0}'': mut modifier is only valid on function declarations", RENDER_NULLABLE_NAME)
        map.put(CfirErrors.NON_EXHAUSTIVE_MATCH, "match expression is not exhaustive. Missing cases: {0}", RENDER_STRING_LIST)
        map.put(
            CfirErrors.UNRESOLVED_REFERENCE,
            "Unresolved reference: ''{0}''.",
            RENDER_STRING,
            NOT_RENDERED,
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
            "Argument type mismatch: actual type is ''{0}'', but ''{1}'' was expected.",
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

    }
}

