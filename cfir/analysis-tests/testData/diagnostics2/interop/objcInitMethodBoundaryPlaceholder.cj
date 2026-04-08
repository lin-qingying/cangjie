// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
@ObjCMirror
open class WidgetMirror {
    public static func buildOk(): WidgetMirror {
        return WidgetMirror()
    }

    <!MISMATCHED_TYPES!>/* suggested message: ObjC init-like method on a mirror type should return the mirror type itself. source: external/cangjie_compiler/src/Sema/NativeFFI/ObjC/TypeCheck/CheckInitMethod.cpp */public static func buildBad(): Int64<!> {
        return 0
    }
}
