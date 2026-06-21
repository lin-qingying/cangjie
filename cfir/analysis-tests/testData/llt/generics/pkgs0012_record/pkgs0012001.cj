


internal import pkgs0012001a.Test

main(): Int64 {
    var value = Test<Int32>(1, 10)
    var b = value.member1 + value.member2
    var c = value.testNormal(false)
    var d = value.testGeneric1(123)
    var e = value.testGeneric2(123, true)
    if (value.member1 == 1 && value.member2 == 10 && b == 11 && c == false && d == 123 && e == true) {
        return 0
    } else {
        return 1
    }
}
