

func f<T>(x: Array<T>) {
    x.size
}

main() {
    f<Int64><!WRONG_NUMBER_OF_ARGUMENTS!>(1, true)<!>
}
