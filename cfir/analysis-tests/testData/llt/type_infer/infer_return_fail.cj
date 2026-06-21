

func <!UNABLE_TO_INFER_RETURN_TYPE!>foo<!>() {
    let temp = 2
    return temp % match (foo) {
        case 2 => 0
        case _ => 1
    }
}

func foo1() {
    let temp = 2
    return foo % match (temp) {
        case 1 => 0
        case _ => 1
    }
}

main(): Int64 {
    return foo() + foo1()
}
