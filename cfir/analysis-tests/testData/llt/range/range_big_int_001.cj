


main(): Int64 {
    var a = 9223372036854775807..9223372036854775807 : -9223372036854775808
    for (i in a) {
        print(i.toString())
    }
    if (a.isEmpty()) {
        return 0
    }
    return 2
}
