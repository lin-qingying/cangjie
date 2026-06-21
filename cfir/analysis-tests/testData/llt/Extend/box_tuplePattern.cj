

main() {
    let t = (1, "1")
    match (t) {
        case (x: ToString, y: ToString) =>
            if (x.toString() != "1" || y.toString() != "1") {
                return 1
            }
    }
    return 0
}
