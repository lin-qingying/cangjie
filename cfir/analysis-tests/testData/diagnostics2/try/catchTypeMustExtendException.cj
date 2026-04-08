// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class NotException {}

func wrongCatchType(): Unit {
    try {
        throw Exception()
    } <!CATCH_TYPE_MUST_EXTEND_EXCEPTION!>/* suggested message: catch pattern type must extend Exception. source: manual_source_zh_cn_handle; external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */catch (e: NotException)<!> {
        println(e)
    }
}
