

let i: Int32 = 10
var lmda1 = {
    x: Int32 =>
    var test: Int32 = 10
    test + x + i
}

main(): Int64 {
    var res1: Int64 = <!TYPE_MISMATCH!>lmda1(3)<!>
    if (res1 != 30) {
        return 1
    }
    return 0
}
