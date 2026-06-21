


func f(x: Float32): Float32 {
    x + 1.0
}

func g(x: Float32): Float32 {
    x * 2.0
}

func run() {
    let x: Float32 = 1.0
    let fg = f ~> g
    let res = fg(x)
    return res
}

main(): Int64 {
    let res = run()
    if (res == 4.000000) {
        return 0
    }
    return 1
}
