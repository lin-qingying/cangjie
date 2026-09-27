class A {
    public operator func ==(right: A): Int64 {
        0
    }
}

main() {
    let x = (A(), 1)
    if (x == (A(), 1)) {}
}
