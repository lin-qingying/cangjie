

func foo3(x!: Int64 = 1, y!: Rune = r'a', z!: Float64 = 0.0): Int32 {
    return 1
}

func foo3(x!: Int32 = 1, y!: Rune, z!: Float64 = 0.0): Int32 {
    return 2
}

main(): Int32 {
    let v: Int32 = 1
    foo3(x: v, y: r'c')
    return foo3(x: 1) - 1
}
