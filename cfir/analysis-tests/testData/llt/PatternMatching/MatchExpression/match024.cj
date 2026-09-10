

main() {
    let x = Some(true)
    match (x) {
        case <!UNREACHABLE_PATTERN!>_: Int64<!> | _: Option<Bool> => 0
        case <!UNREACHABLE_PATTERN!>Some(false)<!> => 1
        case <!UNREACHABLE_PATTERN!>_<!> => 2
    }
}
