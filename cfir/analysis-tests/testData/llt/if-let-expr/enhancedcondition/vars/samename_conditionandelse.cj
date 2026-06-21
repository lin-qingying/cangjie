


main() {
    let a = Some(1)
    if (let Some(b) <- a) {
        <!UNUSED_EXPRESSION!>Some(0)<!>
    } else {
        let b = a
        b
    }
    0
}
