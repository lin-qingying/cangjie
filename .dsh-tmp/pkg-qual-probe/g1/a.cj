package probe

struct S {
    let x: Int64 = 1

    static let y: Int64 = 1

    init(v: Int64) {
        c = 8
    }

    S(let c!: Int32 = 9) {}
}

main(): Int64 {
    let s = S()
    return 0
}
