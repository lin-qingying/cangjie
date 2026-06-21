

interface I {}

main() {
    let _: I = (<!TYPE_MISMATCH!>[1]<!>)
}
