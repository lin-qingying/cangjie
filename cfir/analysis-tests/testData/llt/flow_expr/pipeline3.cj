


func f(x: Float32): Float32 {
    x + 1.0
}

func run() {
    let x: Float32 = 1.0
    let res = x |> f |> f |> f
    print("res:${res}\n")
    return res
}

main(): Int64 {
    let res = run()
    if (res == 4.000000) {
        return 0
    }
    return 1
}
