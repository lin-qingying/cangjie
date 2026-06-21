

main() {
    let r: Any = "fff"
    let a = Some(r)
    if (let Some(b: String) <- a && b.size >= 1) {
        println(b)
    }
    while ((let Some(b: String) <- a && b.size >= 1) && b.size >= 2) {
        println(b)
        break
    }
}
