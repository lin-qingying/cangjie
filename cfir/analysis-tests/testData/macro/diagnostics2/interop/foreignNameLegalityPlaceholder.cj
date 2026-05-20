// RUN_PIPELINE_TILL: FRONTEND
@ObjCImpl
class <!OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS, OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR!>GoodObjCImpl<!> {
    @ForeignName("doThing:withValue:")
    public func doThing(a: Int64, b: Int64): Unit {}
}

@ObjCImpl
class <!OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS, OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR!>MissingForeignNameObjCImpl<!> {
    public func <!OBJC_METHOD_MUST_HAVE_FOREIGN_NAME!>doThing<!>(a: Int64, b: Int64): Unit {}
}

<!JAVA_MIRROR_INTEROPLIB_MUST_BE_IMPORTED!>@JavaMirror
open class JavaBase {}<!>

<!JAVA_MIRROR_SUBTYPE_MUST_BE_ANNOTATED, MISSING_JAVA_INTEROP_ANNOTATION!>class IllegalJavaChild <: JavaBase {}<!>
