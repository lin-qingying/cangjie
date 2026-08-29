


func foo(a: A): Int32 {
    123
    return 1
    1.2
    return <!RETURN_TYPE_MISMATCH!>"hello"<!>
}

class A {}

func bar(b: Int32): Int32 {
    123
    1
    <!CANNOT_CONVERT_LITERAL!>12.0<!>
}
