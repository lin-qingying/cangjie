

func foo(a: ?Int64, b: ?Int64) {
    while (let Some(a) <- a && a > 10 && let Some(b) <- b) {
        return a + b
    }
    -1
}

main() {
    println(foo(Some(18), Some(-11))) // 7
    println(foo(Some(3), Some(5))) // -1
    println(foo(None, None)) // -1
}
