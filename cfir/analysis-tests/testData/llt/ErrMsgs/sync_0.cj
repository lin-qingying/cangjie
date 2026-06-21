


main() {
    let b = synchronized(<!TYPE_MISMATCH!>()<!>) {}
    let c: Bool = synchronized(<!TYPE_MISMATCH!>false<!>) {
        <!TYPE_MISMATCH!>1<!>
    }
    0
}
