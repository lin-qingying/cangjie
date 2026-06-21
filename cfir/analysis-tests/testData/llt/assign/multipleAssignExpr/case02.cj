

func test() {
    var tuple = (1, 2, 3)
    (<!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>tuple[0]<!>, _, <!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>tuple[2]<!>) = (4, 5, 6)
}

main() {
}
