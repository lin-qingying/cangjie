func foo0(a: Int32, b!: Int32 = c + 1, c!: Int32 = a * b) {
    c
}

func foo1(arg0!: Int32, b!: Int32 = arg0 + 1) {
    b
}

func foo2(a: Int32, b!: Int32 = b) {
    c
}

func run(): Int64 {
    foo1(1)
    return 1
}

main(): Int64 {
    return run()
}
