// RUN_PIPELINE_TILL: FRONTEND
@<!UNRESOLVED_REFERENCE!>ObjCImpl<!>
class GoodObjCImpl {
    @<!UNRESOLVED_REFERENCE!>ForeignName<!>["doThing:withValue:"]
    public func doThing(a: Int64, b: Int64): Unit {}
}

@<!UNRESOLVED_REFERENCE!>ObjCImpl<!>
class MissingForeignNameObjCImpl {
    public func doThing(a: Int64, b: Int64): Unit {}
}

@<!UNRESOLVED_REFERENCE!>JavaMirror<!>
open class JavaBase {}

class IllegalJavaChild <: JavaBase {}
