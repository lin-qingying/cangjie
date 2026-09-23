// RUN_PIPELINE_TILL: FRONTEND
@ObjCMirror
open class NativeMirror {
    @ForeignName["ok:"]
    public open func ok(value: Int64): Unit {}
}

@ObjCImpl
class NativeImpl <: NativeMirror {
    @ForeignName["ok:"]
    public override func ok(value: Int64): Unit {}
}

@ObjCImpl
class MissingMirrorInheritance {
    public func ok(value: Int64): Unit {}
}
