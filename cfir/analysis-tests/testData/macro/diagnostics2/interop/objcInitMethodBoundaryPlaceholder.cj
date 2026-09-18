// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR
@<!UNRESOLVED_REFERENCE!>ObjCMirror<!>
open class WidgetMirror {
    @<!UNRESOLVED_REFERENCE!>ObjCInit<!>
    public static func buildOk(): WidgetMirror {
        return WidgetMirror()
    }

    @<!UNRESOLVED_REFERENCE!>ObjCInit<!>
    public static func buildBad(): Int64 {
        return 0
    }
}
