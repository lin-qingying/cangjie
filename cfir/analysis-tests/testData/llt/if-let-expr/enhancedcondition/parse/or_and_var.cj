

main() {
    let a = Some(3)
    if ((true || false) && let Some(b) <- a) {
        0
    } else {
        1
    }
}
