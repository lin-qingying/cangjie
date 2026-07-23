

func f(x: Array<Int64>) {
    x.size
}

main() {
    f<!WRONG_NUMBER_OF_ARGUMENTS!>([1, 2], 3)<!>
}
