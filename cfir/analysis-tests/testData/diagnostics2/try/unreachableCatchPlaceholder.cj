// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
open class ParentEx <: Exception {}
class ChildEx <: ParentEx {}

func unreachableCatch(): Unit {
    try {
        throw ChildEx()
    } catch (e: ParentEx) {
        println(e)
    } <!UNREACHABLE_CATCH_BLOCK!>/* suggested message: catch block is unreachable because an earlier catch already handles the same exception family. source: manual_source_zh_cn_handle; external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */catch (e: ChildEx)<!> {
        println(e)
    }
}
