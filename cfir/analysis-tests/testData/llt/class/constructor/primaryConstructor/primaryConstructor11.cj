

main(): Int64 {
    return run()
}

class Test {
    public let width2: Int8
    public let length2: Int8

    Test(width: Int64, length: Int64) {
        width2 = <!ASSIGNMENT_TYPE_MISMATCH!>width<!>
        length2 = <!ASSIGNMENT_TYPE_MISMATCH!>length<!>
    }
}

func run(): Int64 {
    Test(2, 3)
    return 1
}
