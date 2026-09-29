


func run(): Int64 {
    var i1: Int64 = -9223372036854775808
    var i2: Int64 = - 9223372036854775808
    var i3: Int64 = <!LITERAL_NUMERIC_OVERFLOW!>- 9223372036854775809<!>
    return 0
}

main(): Int64 {
    return run()
}
