

func foo(a: ?Int64, b: ?Int64) {
    if (let Some(a) <- a && a > 10 && let Some(b) <- b) {
        a + b
    } else {
        -1
    }
}

main() {
    println(foo(Some(18), Some(-11)))
    println(foo(Some(3), Some(5)))
    println(foo(None, None))
}
