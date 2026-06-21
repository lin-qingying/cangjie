

enum A {
    B | C | D
}

main() {
    let a = B
    println(foo(a))
    println(foo(D))
}

func foo(a: A) {
    if (let B | C <- a) {
        1
    } else {
        -1
    }
}
