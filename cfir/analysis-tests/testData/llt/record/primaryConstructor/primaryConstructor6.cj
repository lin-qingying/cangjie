

main(): Int64 {
    return run()
}

struct Test {
    public let width2: Rune
    public let length2: Rune

    Test(width: Int64, length: Int64) {
        width2 = <!TYPE_MISMATCH!>width<!>
        length2 = <!TYPE_MISMATCH!>length<!>
    }
}

func run(): Int64 {
    Test(2, 3)
    return 1
}
