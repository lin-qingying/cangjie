

func foo(a: ?Int64, b: ?Int64) {
    while (true && (let Some(_) <- a || let None <- b)) {
        return 8
    }
    -1
}

func Go(a: Int64) {
    a > 3
}

main() {
    println(foo(None, Some(3))) // -1
    println(foo(Some(8), None)) // 8
}
