

func foo(c: ?Int) {
    if (let Some(_)|None <- c || let None <- c) {
        println(0)
    }
}

main() {
    foo(3)
}
