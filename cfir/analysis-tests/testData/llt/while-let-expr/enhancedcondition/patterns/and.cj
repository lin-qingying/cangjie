

func foo(a: ?Int64) {
    while (let Some(a) <- a && a > 10) {
        return a + 1
    }
    return -1
}

main() {
    println(foo(Some(18))) // 19
    println(foo(Some(3))) // -1
    println(foo(None))
}
