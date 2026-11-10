func withDefault(g: (x: Int64 = 1) -> Bool): Bool {
    return g(1)
}

main(): Int64 {
    return 0
}
