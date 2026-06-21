


internal import pkgs0011001a.*

main(): Int64 {
    var value = Test<Int32>(1)
    var b = value.member + 10
    var c = value.testNormal(false)
    var d = value.testGeneric1(123)
    var e = value.testGeneric2(123, true)
    if (value.member == 1 && b == 11 && c == false && d == 123 && e == true) {
        return 0
    } else {
        return 1
    }
}
