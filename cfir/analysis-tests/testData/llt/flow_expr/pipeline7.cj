


class C {
    static func f() {
        return 0
    }
    static func f(i: Int64) {
        return i
    }
}

main() {
    let v = 2 |> C.f
    if (v != 2) {
        return 1
    }
    return 0
}
