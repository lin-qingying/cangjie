// RUN_PIPELINE_TILL: FRONTEND
func takesInt(x: Int64): Int64 {
    return x
}

func takesInt1<T>(x: T): T {
    return x
}

main(): Unit {
    takesInt1<Int>
    takesInt
    takesInt1<Float64>(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    takesInt(<!ARGUMENT_TYPE_MISMATCH!>true<!>)
}
