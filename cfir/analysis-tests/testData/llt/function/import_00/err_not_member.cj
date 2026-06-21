// FILE: m1/p1.cj


package m1.p1

public func f() {
    1
}

// FILE: err_not_member.cj



import m1.p1

main() {
    "${p1.<!NOT_MEMBER_OF!>g<!>()}"
    0
}
