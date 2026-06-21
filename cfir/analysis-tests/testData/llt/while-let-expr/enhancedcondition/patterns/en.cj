

enum A {
    B | C | D
}

main() {
    let a = B
    println(foo(a))
    println(foo(D))
}

func foo(a: A) {
    while (let B | C <- a) {
        return 1
    }
    -1
}
