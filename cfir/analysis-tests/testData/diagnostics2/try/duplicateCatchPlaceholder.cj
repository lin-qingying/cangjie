// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class DomainEx <: Exception {}

func duplicateCatch(): Unit {
    try {
        throw DomainEx()
    } catch (e: DomainEx) {
        println(e)
    } <!DUPLICATE_CATCH_BLOCK!>/* suggested message: catch block duplicates an exception type already handled by an earlier catch. source: manual_source_zh_cn_handle; external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */catch (e: DomainEx)<!> {
        println(e)
    }
}
