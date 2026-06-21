

func f(x: Int64): Bool {
    false
}

func f(x: Int32): Int64 {
    -10
}

func f(x: Int8): UInt64 {
    10
}

main() {
    let a = 2 ** f(2)
    println(a)
    0
}
