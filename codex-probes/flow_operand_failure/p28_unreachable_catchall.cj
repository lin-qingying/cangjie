enum Color {
    | Red
    | Green
}

main() {
    let c = Color.Red
    let n = match (c) {
        case Color.Red => 1
        case Color.Green => 2
        case _ => 3
    }
    println("${n}")
}
