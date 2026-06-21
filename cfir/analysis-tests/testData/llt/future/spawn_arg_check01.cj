

main(): Int64 {
    let context = 6606i64

    let th = spawn (<!TYPE_MISMATCH!>context<!>) {
        return 25
    }
    return 0
}
