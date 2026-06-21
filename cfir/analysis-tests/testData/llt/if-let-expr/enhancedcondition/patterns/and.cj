

func foo(a: ?Int64) {
    if (let Some(a) <- a && a > 10) {
        a + 1
    } else {
        -1
    }
}

main() {
    println(foo(Some(18)))
    println(foo(Some(3)))
    println(foo(None))
}
