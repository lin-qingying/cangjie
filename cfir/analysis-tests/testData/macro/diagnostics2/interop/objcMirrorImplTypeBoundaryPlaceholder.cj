// RUN_PIPELINE_TILL: FRONTEND
@<!UNRESOLVED_REFERENCE!>ObjCMirror<!>
open class NativeMirror {
    @<!UNRESOLVED_REFERENCE!>ForeignName<!>["ok:"]
    public open func ok(value: Int64): Unit {}
}

@<!UNRESOLVED_REFERENCE!>ObjCImpl<!>
class NativeImpl <: NativeMirror {
    @<!UNRESOLVED_REFERENCE!>ForeignName<!>["ok:"]
    public override func ok(value: Int64): Unit {}
}

@<!UNRESOLVED_REFERENCE!>ObjCImpl<!>
class MissingMirrorInheritance {
    public func ok(value: Int64): Unit {}
}
