

main() {
    var a: Int64
    var b: Int64
    if ((1, <!USED_BEFORE_INITIALIZATION!>b<!>) == (1, 2)) {
        print(1)
    }
    if (<!USED_BEFORE_INITIALIZATION!>b<!> == 1) {
        a = 1
    }
    return <!USED_BEFORE_INITIALIZATION!>b<!> - a - a
}
