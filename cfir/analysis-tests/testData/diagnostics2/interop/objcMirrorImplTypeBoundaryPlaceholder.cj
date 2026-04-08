// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
@ObjCMirror
open class NativeMirror {
    @ForeignName("ok:")
    public open func ok(value: Int64): Unit {}
}

@ObjCImpl
class NativeImpl <: NativeMirror {
    @ForeignName("ok:")
    public override func ok(value: Int64): Unit {}
}

@ObjCImpl
class MissingMirrorInheritance {
    <!OBJC_IMPL_MUST_INHERIT_MIRROR!>/* suggested message: ObjC impl type must inherit from a compatible ObjC mirror type. source: external/cangjie_compiler/src/Sema/NativeFFI/ObjC/AfterTypeCheck/Interop/CheckImplTypes.cpp */public func ok(value: Int64): Unit {}<!>
}
