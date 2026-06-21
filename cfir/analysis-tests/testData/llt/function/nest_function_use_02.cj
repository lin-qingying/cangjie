


func <!UNABLE_TO_INFER_RETURN_TYPE!>foo<!><T>(a: T) {
    bar<Int64>(1)
    a
}

func bar<T>(a: T) {
    let b = {a: Any => ()}(<!NO_MATCH_FUNCTION_DECLARATION_FOR_REF!>f<!>oo<Int64>) // Use unknown as reference
    a
}

func baz<T>(a: T): T {
    a
}

main(): Unit {
    bar(1)
}
