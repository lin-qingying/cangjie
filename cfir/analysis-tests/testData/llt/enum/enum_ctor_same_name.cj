

enum E {
    | C
    | C(Int64)

    static func fromInt(x: Int64) {
        C(x)
    }
}

main() {
    let c = E.fromInt(123)
    ()
}
