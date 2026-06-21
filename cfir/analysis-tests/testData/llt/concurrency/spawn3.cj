


main(): Int64 {
    let num = 1
    spawn {
        => return num + 10
    }
    return 0
}
