


main() {
    let a = 1
    let b = Some(3)

    if (true && (a) > 3) {
        1
    } else if (let Some(x) <- b && (x) >= 3) {
        0
    } else {
        -1
    }
}
