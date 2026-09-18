// RUN_PIPELINE_TILL: FRONTEND

@OverflowWrapping
func wrappingOverflow(): Int64 {
    9223372036854775807 + 1
}

@OverflowSaturating
func saturatingOverflow(): Int64 {
    9223372036854775807 + 1
}

@OverflowThrowing
func throwingOverflow(): Int64 {
    <!CONST_EVAL_ARITHMETIC_OVERFLOW!>9223372036854775807 + 1<!>
}
