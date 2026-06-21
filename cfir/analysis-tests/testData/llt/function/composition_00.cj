

func composition(f: (Int64) -> Int64, g: (Int64) -> Int64): (Int64) -> Int64 {
    return {x: Int64 => f(g(x))}
}

func f(x: Int64): Int64 {
    x + x
}

func g(x: Int64): Int64 {
    x * x
}

main() {
    let fg = f ~> g
    fg(3)
}
