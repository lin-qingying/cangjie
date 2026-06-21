

extend Int32 {
    public static func sub(a: Int64): Int64 {
        return a - 1
    }
}

main() {
    let a: Int64 = 1
    return Int32.sub(a)
}
