


enum EE {
    A1 | B1(Int64)

    const func get() {
        match (this) {
            case A1 => 1
            case _ => 2
        }
    }
}

main() {
    A1.get()

    return 0
}
