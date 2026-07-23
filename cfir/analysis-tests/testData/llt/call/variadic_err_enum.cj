

enum E {
    A(Array<Int64>)

    func f(x!: E = A<!WRONG_NUMBER_OF_ARGUMENTS!>(1, 2)<!>) {}
}

main() {}
