

enum E {
    EE(Int64, String)
}

main() {
    let t = EE(1, "1")
    match (t) {
        case EE(x: ToString, y: ToString) =>
            if (x.toString() != "1" || y.toString() != "1") {
                return 1
            }
    }
    return 0
}
