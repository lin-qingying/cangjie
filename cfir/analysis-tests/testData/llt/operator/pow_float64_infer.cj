

func f(x: Int64): Float64 {
    10.0
}

func f(x: Int32): Float32 {
    -10.0
}

func f(x: Int16): Float16 {
    -10.0
}

func g(x: Int64): Int64 {
    10
}

func g(x: Int32): Int32 {
    -10
}

func g(x: Int16): Int16 {
    -10
}

main() {
    let a = 2.0 ** f(2)
    println(a)
    
    0
}
