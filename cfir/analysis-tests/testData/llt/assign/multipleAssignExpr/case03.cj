

func test() {
    var a = 1
    var b = 2
    var c = 3
    <!TYPE_MISMATCH!>(a, <!UNRESOLVED_REFERENCE!>d<!>, <!UNRESOLVED_REFERENCE!>e<!>) = (1, 2, 3)<!>
    <!TUPLE_PATTERN_NOT_MATCH!>(a, b, c, <!UNRESOLVED_REFERENCE!>d<!>) = (1, 2)<!>
    <!TUPLE_PATTERN_NOT_MATCH!>(a, b, c) = (1, 2, 3, 4)<!>
    <!TYPE_MISMATCH!>(a, b, c) = (1, 2, (3, 4))<!>
    <!TUPLE_PATTERN_NOT_MATCH!>(a, _, c, <!UNRESOLVED_REFERENCE!>e<!>) = ((1, 2), (3, 4), (5, 6))<!>
}

main() {
}
