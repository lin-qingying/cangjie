

enum E {
    A(Array<Int64>)

    func f(x!: E = A(1, <!TOO_MANY_ARGUMENTS!>2<!>)) {}
}

main() {}
