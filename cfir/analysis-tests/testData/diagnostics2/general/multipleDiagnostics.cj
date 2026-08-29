// RUN_PIPELINE_TILL: FRONTEND
func takesInt(x: Int64): Int64 {
    return x
}

func badReturn(): Int64 {
    return <!CANNOT_CONVERT_LITERAL!>true<!>
}

main(): Unit {
    takesInt(<!CANNOT_CONVERT_LITERAL!>true<!>)
}
