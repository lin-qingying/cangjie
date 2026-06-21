

main() {
    let a = Some(1)
    let b = Some(3)

    if (let Some(c) <- a && let Some(d) <- b && c < d) {
        println(c + d)
    }
    while ((let Some(c) <- a && let Some(d) <- b) && c < d) {
        println(c + d)
        break
    }
}
