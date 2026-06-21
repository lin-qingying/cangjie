

func f<T>(x: T) {
    match (x) {
        case _: Int64 => 0 // reachable
        case _: T => 1
    }
}

main() {
    f(0)
}
