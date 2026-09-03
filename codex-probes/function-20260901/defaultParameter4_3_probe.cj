func foo0(a: Int32, b!: Int32, c!: Int32 = 2): Unit {
    return
}

func foo1(arg0!: Int32, b!: Int32 = arg0 + 1): Unit {
    return
}

func foo2(a: Int32, b!: Int32 = a): Unit {
    return
}

main(): Int64 {
    foo0()
    foo0(1)
    foo0(1, 2)
    foo0(1, 2, 3)
    foo0(1, b: 2, c: 3, 4)
    foo1(1)
    foo1(1, 2)
    foo1(1, 2, 3)
    foo2(1)
    foo2(1, 2)
    foo2(1, 2, 3)
    return 0
}
