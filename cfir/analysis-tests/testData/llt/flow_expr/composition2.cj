


func f(x: Int32): Float32 {
    Float32(x) + 1.0
}

func g(x: Float32): Int32 {
    Int32(x * 2.0)
}

func run() {
    let x: Int32 = 1
    let fg = f ~> g
    let res = fg(x)
    return res
}

main(): Int64 {
    let res = run()
    if (res == 4) {
        return 0
    }
    return 1
}
