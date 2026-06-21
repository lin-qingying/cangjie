

main(): Int64 {
    var str1: String
    var str2 = <!USED_BEFORE_INITIALIZATION!>str1<!> + "4"
    print(str2)
    return 0
}
