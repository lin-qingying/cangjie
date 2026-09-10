

main() {
    let x = (true, true)
    match (x) {
        case (true, true) | <!UNREACHABLE_PATTERN!>(false, false)<!> => 0
        case <!UNREACHABLE_PATTERN!>(true, false)<!> | <!UNREACHABLE_PATTERN!>(false, true)<!> => 1
    }
}
