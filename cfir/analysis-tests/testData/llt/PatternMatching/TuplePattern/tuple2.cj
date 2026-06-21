


main(): Int64 {
    match (1) {
        case <!TUPLE_PATTERN_NOT_MATCH!>(k, v)<!> => 1
        case _ => <!INVALID_LOOP_CONTROL!>continue<!>
    }
    0
}
