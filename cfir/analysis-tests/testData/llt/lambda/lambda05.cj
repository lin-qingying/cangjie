

main(): Int64 {
    if (case02()(1) != ()) {
        return 2
    }
    return 0
}

func case02() {
    var a: Int32 = 1
    var c: (Int32) -> Unit = {x: Int32 => x}
    return c
}
