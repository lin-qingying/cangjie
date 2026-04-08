// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
@ObjCImpl
class GoodObjCImpl {
    @ForeignName("doThing:withValue:")
    public func doThing(a: Int64, b: Int64): Unit {}
}

@ObjCImpl
class MissingForeignNameObjCImpl {
    <!OBJC_METHOD_MUST_HAVE_FOREIGN_NAME!>/* suggested message: ObjC exported method with multiple parameters must declare @ForeignName. source: external/cangjie_compiler/src/Sema/NativeFFI/ObjC/TypeCheck/CheckForeignName.cpp */public func doThing(a: Int64, b: Int64): Unit {}<!>
}

@Java
open class JavaBase {}

<!EXTEND_JAVA_TYPE_NOT_ALLOWED!>/* suggested message: extending a Java interop type requires dedicated NativeFFI legality checks. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md; external/cangjie_compiler/src/Sema/NativeFFI/Java/TypeCheck/InheritanceChecker.cpp */class IllegalJavaChild <: JavaBase {}<!>
