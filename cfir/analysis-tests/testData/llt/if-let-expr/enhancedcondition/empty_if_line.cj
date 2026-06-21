

func foo(c: ?Int) {
    if (let Some(_) <- c) {}
    else if (let None <- c) {
        ()
    }
}

main() {
    foo(3)
    foo(None)
}
