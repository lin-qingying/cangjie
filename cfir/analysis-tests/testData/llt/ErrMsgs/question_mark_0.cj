


class C {}

main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    let optLam: Option<() -> Unit> = {=>}
    let a = optLam?<!WRONG_NUMBER_OF_ARGUMENTS!>(1)<!>
    a + 1
    (err)?(1)

    let optTup = Some((1, true))
    let b = optTup?[<!BUILTIN_INDEX_IN_BOUND!>2<!>]
    b - 2
    (err)?[1]

    let optC = Some(C())
    let c = optC?.<!NOT_MEMBER_OF!>x<!>
    c * 3
    (err)?.x

    0
}
