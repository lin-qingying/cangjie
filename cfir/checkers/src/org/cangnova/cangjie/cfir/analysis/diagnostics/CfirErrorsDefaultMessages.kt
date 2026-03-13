package org.cangjie.cfir.analysis.diagnostics

import org.cangjie.cfir.diagnostics.CjDiagnosticFactoryToRendererMap
import org.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory

object CfirErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP: CjDiagnosticFactoryToRendererMap by CjDiagnosticFactoryToRendererMap("FIR") { map ->
        map.put(CfirErrors.INVALID_DECLARATION, "{0}", null)
        map.put(CfirErrors.TYPES_ERROR_RECOVERY, "{1}", null, null)
        map.put(CfirErrors.IMPORT_TARGET_NOT_FOUND, "{1}", null, null)
        map.put(CfirErrors.IMPORT_CONFLICT, "{1}", null, null)
        map.put(CfirErrors.IMPORT_ALIAS_CONFLICT, "{1}", null, null)
        map.put(CfirErrors.SUPER_TYPES_SELF_REFERENCE, "{1}", null, null)
        map.put(CfirErrors.SUPER_TYPES_DUPLICATE, "{1}", null, null)
        map.put(CfirErrors.ILLEGAL_EXTENDED_TYPE, "{1}", null, null)
        map.put(CfirErrors.EXTEND_DUPLICATE_INTERFACE, "{1}", null, null)
        map.put(CfirErrors.EXTEND_NOT_INTERFACE, "{1}", null, null)
        map.put(CfirErrors.INTERFACE_CANNOT_INHERIT_CLASS, "{1}", null, null)
        map.put(CfirErrors.MULTIPLE_CLASS_SUPER_TYPES, "{1}", null, null)
        map.put(CfirErrors.STATUS_MODIFIER_LEGALITY, "{1}", null, null)
    }
}
