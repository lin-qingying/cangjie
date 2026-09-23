// RUN_PIPELINE_TILL: FRONTEND
@ObjCImpl
class GoodObjCImpl {
    @ForeignName["doThing:withValue:"]
    public func doThing(a: Int64, b: Int64): Unit {}
}

@ObjCImpl
class MissingForeignNameObjCImpl {
    public func doThing(a: Int64, b: Int64): Unit {}
}

<!JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED!>@JavaMirror
open class JavaBase {}<!>

class IllegalJavaChild <: JavaBase {}
