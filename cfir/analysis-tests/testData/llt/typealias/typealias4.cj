

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type A<T, Y, U><!> = Int64

main() {
    var a: A<Bool, Rune, Int64> = 10;
    var b: Int32 = 20;
    var c = a <!INVALID_BINARY_OPERATOR!>+<!> b;
    return 0;
}
