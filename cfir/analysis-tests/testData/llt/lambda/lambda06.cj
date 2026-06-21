

main(): Int64 {
    if (case01({x: Int32 => x}) != ()) {
        return 2
    }
    return 0
}

func case01(x: (Int32) -> Unit) {
    return x(1)
}
