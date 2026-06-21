

func Foo(a!: Int32 = 8, b!: Int32 = a) {
    return a + b
}

func run(): Int64 {
    var foo: Int32 = Foo()
    if (foo == 16) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}
