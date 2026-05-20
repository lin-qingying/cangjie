// RUN_PIPELINE_TILL: FRONTEND
// DUMP_MACRO_EXPANDED_CFIR
@ObjCMirror
open class WidgetMirror {
    @ObjCInit
    public static func buildOk(): WidgetMirror {
        return WidgetMirror()
    }

    @ObjCInit
    public static func buildBad(): <!TYPE_MISMATCH!>Int64<!> {
        return 0
    }
}
