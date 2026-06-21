

main() {
    let x: (Int32, Int32) = if (true) {
        (0, 0)
    } else {
        <!TYPE_MISMATCH!>(0, 0, 0)<!>
    }
}
