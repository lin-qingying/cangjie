// RUN_PIPELINE_TILL: FRONTEND
func overloaded(x: Int64): Int64 { x }
func overloaded(x: Bool): Bool { x }

func ambiguousReference(): Unit {
    let ref = <!AMBIGUOUS_USE!>overloaded<!>
    let _ = ref
}
