main() {
    let err = 1 && false
    err ~> err
    0
}
