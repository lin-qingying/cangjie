

var a1: (Int32, Int32) -> Int32 = {a, b => a + b}
var a2: (Int32, Int32) -> Int32 = {<!TYPE_MISMATCH!>a: Int16<!>, b => a + b}

main(): Int64 {
    if (a1(1, 1) != 2) {
        return 1
    }
    if (a2(1, 1) != 2) {
        return 1
    }
    return 0
}
