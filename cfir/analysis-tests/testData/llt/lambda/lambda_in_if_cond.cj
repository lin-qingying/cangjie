


main(): Int64 {
    print({=> "abc"}())
    if ({=> "abc"}() != "abc") {
        return 1
    }
    return 0
}
