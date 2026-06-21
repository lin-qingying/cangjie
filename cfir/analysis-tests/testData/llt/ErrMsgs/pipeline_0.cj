


class C {
    operator func ()(a: Int32) {}
}

main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    1 |> C()
    err |> err

    (err |> err) + false
    0
}
