

let x = 0

main() {
    let <!TUPLE_PATTERN_NOT_MATCH!>(x, _)<!> = match (x) {
        case _ => 0
    }
}
