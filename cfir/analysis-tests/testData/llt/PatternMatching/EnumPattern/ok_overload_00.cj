

enum E {
    | a(Int64)
    | a
}

func f(e: E) {
    match (e) {
        case E.a => 0
        case E.a(_) => 1
    }
}

main() {
    f(E.a)
}
