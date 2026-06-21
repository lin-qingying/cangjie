


func f(x: Float32): Float32 {
    x + 1.0
}

func g(x: Float32): Float32 {
    x * 2.0
}

func run() {
    let x: Float32 = 1.0
    let fgf = f ~> g ~> f
    let res = fgf(x)
    print("res: ${res}")
    return res
}

main(): Int64 {
    let res = run()
    if (res == 5.000000) {
        return 0
    }
    return 1
}
