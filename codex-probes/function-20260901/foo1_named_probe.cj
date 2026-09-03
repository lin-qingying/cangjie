func foo1(arg0!: Int32, b!: Int32 = arg0 + 1): Unit {
    return
}

main(): Int64 {
    foo1(1)
    foo1(arg0: 1)
    return 0
}
