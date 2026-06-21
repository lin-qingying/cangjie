


internal import pkgs0010001a.*

main(): Int64 {
    var value = Test<Int32>(1)
    var b = value.member1 + 10

    if (value.member1 == 1 && b == 11) {
        return 0
    } else {
        return 1
    }
}
