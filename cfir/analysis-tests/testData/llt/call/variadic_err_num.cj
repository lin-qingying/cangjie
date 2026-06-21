

func f(x: Array<Int64>) {
    x.size
}

main() {
    f([1, 2], <!TOO_MANY_ARGUMENTS!>3<!>)
}
