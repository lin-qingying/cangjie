// RUN_PIPELINE_TILL: FRONTEND
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
