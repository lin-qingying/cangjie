


main(): Int64 {
    var a = 1
    while (true) {
        func test() {
            if (true) {
                <!INVALID_LOOP_CONTROL!>break<!>
            }
        }
        test()
    }
    return 0
}
