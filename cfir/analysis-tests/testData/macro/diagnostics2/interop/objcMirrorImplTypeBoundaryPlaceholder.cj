// RUN_PIPELINE_TILL: FRONTEND
@ObjCMirror
open class NativeMirror {
    @ForeignName("ok:")
    public open func ok(value: Int64): Unit {}
}

@ObjCImpl
class NativeImpl <: NativeMirror {
    <!FOREIGN_NAME_APPEARED_IN_CHILD!>@ForeignName<!>("ok:")
    public override func ok(value: Int64): Unit {}
}

@ObjCImpl
class <!OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS, OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR!>MissingMirrorInheritance<!> {
    public func ok(value: Int64): Unit {}
}
