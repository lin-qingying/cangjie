

func foo(): Int32 {
    return 1
}

var a: Int32 = foo<!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>
