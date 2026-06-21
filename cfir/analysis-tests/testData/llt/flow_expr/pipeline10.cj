

main() {
    let f: (Float32) -> Unit = {_ =>}
    (0.0 / 1.0) - <!UNRESOLVED_REFERENCE!>x<!> |> f
}
