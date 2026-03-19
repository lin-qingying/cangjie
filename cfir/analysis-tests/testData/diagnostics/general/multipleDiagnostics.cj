// RUN_PIPELINE_TILL: FRONTEND
func takesInt(x: Int64): Int64 {
    return x
}

func badReturn(): Int64 {
    return <!RETURN_TYPE_MISMATCH!>true<!>
}



main() {

    takesInt(<!ARGUMENT_TYPE_MISMATCH!>true<!>)
}

