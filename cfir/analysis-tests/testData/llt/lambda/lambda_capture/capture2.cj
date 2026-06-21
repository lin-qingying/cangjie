


main(): Int64 {
    var a = 1
    {
        => {
            =>
            a = 3
            a
        }()
    }()
    if (a == 3) {
        return 0
    }
    return 1
}
