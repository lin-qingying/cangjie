


enum D {
    d(Int64) | d
}

enum E {
    e(D) | e
}

main(): Int64 {
    var a: E = e(d(1))
    match (a) {
        case e(d) => 1
        case e => 2
    }
}

