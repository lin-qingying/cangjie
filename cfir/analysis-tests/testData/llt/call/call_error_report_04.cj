

func f<T>(a: Option<T>) {}

main() {
    f<Int64>(None) // ok
    f(Option<Int64>.None) // ok

    f<Nothing>(None) // ok

    f(Option<Nothing>.None)
}
