// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
open class ParentEx <: Exception {}
class ChildEx <: ParentEx {}
class SiblingEx <: ParentEx {}

func parentThenChild(): Unit {
    try {
        throw ChildEx()
    } catch (e: ParentEx) {
        println(e)
    } <!UNREACHABLE_CATCH_BLOCK!>/* suggested message: later catch block is unreachable because an earlier catch already handles that exception family. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */catch (e: ChildEx)<!> {
        println(e)
    }
}

func duplicateSibling(): Unit {
    try {
        throw SiblingEx()
    } catch (e: SiblingEx) {
        println(e)
    } <!DUPLICATE_CATCH_BLOCK!>/* suggested message: duplicate catch block for the same exception type. source: external/cangjie_compiler/src/Sema/TypeCheckExpr/TryExpr.cpp */catch (e: SiblingEx)<!> {
        println(e)
    }
}
