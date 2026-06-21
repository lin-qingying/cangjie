


main(): Int64 {
    let num = 1
    let fut = spawn {
        => return num + 10
    }
    return 0
}
