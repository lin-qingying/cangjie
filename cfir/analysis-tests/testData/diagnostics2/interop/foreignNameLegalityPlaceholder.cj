// RUN_PIPELINE_TILL: FRONTEND
@ObjCImpl
class GoodObjCImpl {
    @ForeignName("doThing:withValue:")
    public func doThing(a: Int64, b: Int64): Unit {}
}

@ObjCImpl
class MissingForeignNameObjCImpl {
    public <!OBJC_METHOD_MUST_HAVE_FOREIGN_NAME!>func doThing<!>(a: Int64, b: Int64): Unit {}
}

@JavaMirror
open class JavaBase {}

class <!JAVA_MIRROR_SUBTYPE_MUST_BE_ANNOTATED!>IllegalJavaChild<!> <: JavaBase {}
