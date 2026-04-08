// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func overloaded(x: Int64): Int64 { x }
func overloaded(x: Bool): Bool { x }

func ambiguousReference(): Unit {
    let ref = <!AMBIGUOUS_FUNCTION_REFERENCE!>/* suggested message: function reference is ambiguous because multiple overloads match the expected shape. source: manual_source_zh_cn_function_call_desugar; external/cangjie_compiler/src/Sema/TypeCheckCall.cpp */overloaded<!>
    println(ref)
}
