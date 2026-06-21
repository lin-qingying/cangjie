

func foo(c: Int) {
    if (let _ <- c) {
        println(c)
    }
}

func bar(c: Int) {
    if (let _ | _ <- c) {
        println(c)
    }
}

main() {
    foo(3)
    bar(7)
}
