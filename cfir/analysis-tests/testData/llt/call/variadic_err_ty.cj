

func f<T>(x: Array<T>) {
    x.size
}

main() {
    f<Int64>(1, <!TOO_MANY_ARGUMENTS!>true<!>)
}
