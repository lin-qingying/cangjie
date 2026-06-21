

main() {
    let a = Some(1)
    if (let Some(a) <- a) {
        Some(0)
    } else {
        let b = a
        b
    }
    0
}
