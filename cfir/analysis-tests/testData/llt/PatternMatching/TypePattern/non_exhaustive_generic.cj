

func f<T>(x: T) {
    match (<!NON_EXHAUSTIVE_MATCH!>x<!>) {
        case _: Int64 => 0 // reachable
    }
}

main() {
    f(0)
}
