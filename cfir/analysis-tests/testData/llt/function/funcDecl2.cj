


func test() {}

func test1(a!: Int32 = 2, b!: Int32 = 2) {}

func help() {
    return 0
}

func help2(): Unit {
    return <!RETURN_TYPE_MISMATCH!>0<!>
}

func help3(): Unit {
    return
}

func exit() {
    if (true) {
        return 0
    } else {
        return ""
    };
    <!INCOMPATIBLE_FUNC_BODY_AND_RETURN_TYPE!>return<!>
}
//func exit2() {if (true {return 0} else) {}}
func exit3() {
    if (true) {
        return
    } else {
        return
    };
    return
}

main() {
    test1(a: 1, b: 1)
    test1(<!NEED_NAMED_ARGUMENT!>1<!>, b: 1)
    test1(b: 1, a: 1)
    test1()
    test1(b: 2)
    test1(a: 1)
    test1(a: 1, <!MIXING_NAMED_AND_POSITIONAL_ARGUMENTS!>1<!>)
    test1(<!NEED_NAMED_ARGUMENT!>1<!>, a: 1)
    test1(<!NEED_NAMED_ARGUMENT!>""<!>, "")
    test1<!WRONG_NUMBER_OF_ARGUMENTS!>("", "", "")<!>
    return 0
}

func fun(a: Int32): Int32 {
    return a
}

func callFunc(myfunc: (Int32, Int32) -> Int32): Int32 {
    return myfunc<!WRONG_NUMBER_OF_ARGUMENTS!>(0)<!>
}

func foo(): Int32 {
    return callFunc(<!NO_MATCH_FUNCTION_DECLARATION_FOR_REF!>f<!>un)
}

func bar() {
    return true;
    <!INCOMPATIBLE_FUNC_BODY_AND_RETURN_TYPE!>1<!>
}

func baz() {
    while (true) {
        return false
    };
    <!INCOMPATIBLE_FUNC_BODY_AND_RETURN_TYPE!>1<!>
}
