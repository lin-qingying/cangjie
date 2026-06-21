

func foo(a: ?Int64, b: ?Int64) {
    if (true && false || let None <- b) {
        8
    } else {
        -1
    }
}

func Go(a: Int64) {
    a > 3
}

main() {
    println(foo(Some(15), Some(3)))
    println(foo(Some(8), None))
}
