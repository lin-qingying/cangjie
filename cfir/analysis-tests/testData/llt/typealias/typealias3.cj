

<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>t<!>ype A<T> = Int64

main() {
    var a: A<Bool> = 10;
    var b: Int32 = 20;
    var c = a <!INVALID_BINARY_OPERATOR!>+<!> b;
    return 0;
}
