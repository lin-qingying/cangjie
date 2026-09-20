

func test() {
    var a = 1
    var b = 2
    var c = 3
    (a, <!UNRESOLVED_REFERENCE!>d<!>, <!UNRESOLVED_REFERENCE!>e<!>) = <!TYPE_MISMATCH!>(1, 2, 3)<!>
    (a, b, c, <!UNRESOLVED_REFERENCE!>d<!>) = <!TYPE_MISMATCH!>(1, 2)<!>
    (a, b, c) = <!TYPE_MISMATCH!>(1, 2, 3, 4)<!>
    (a, b, c) = <!TYPE_MISMATCH!>(1, 2, (3, 4))<!>
    (a, _, c, <!UNRESOLVED_REFERENCE!>e<!>) = <!TYPE_MISMATCH!>((1, 2), (3, 4), (5, 6))<!>
}

main() {
}
