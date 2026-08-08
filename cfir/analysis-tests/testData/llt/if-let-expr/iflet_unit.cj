

main() {
    var a = if (let a <- 1) {
        1
    }
    var res = match (a) {
        case x: Unit => 0
        case <!UNREACHABLE_PATTERN!>_<!> => 1
    }
    return res
}
