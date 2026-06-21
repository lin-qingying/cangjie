

enum E1 {
    EE | ER(Int32)

    public prop a: Int64 {
        get() {
            match (this) {
                case EE => print("get EE\n")
                case ER(x) => print("get ER ${x}\n")
            }
            0
        }
    }
}

main(): Int64 {
    let a = EE
    let b = ER(3)
    b.a
    EE.a
    return 0
}
