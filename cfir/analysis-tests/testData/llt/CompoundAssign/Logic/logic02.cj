


main(): Int64 {
    var a = 2
    var b = 15
    <!TYPE_INCOMPATIBLE!>a<!> &&= b
    <!TYPE_INCOMPATIBLE!>a<!> ||= b
    return 0
}
