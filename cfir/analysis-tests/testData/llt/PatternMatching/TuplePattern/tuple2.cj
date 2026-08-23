



main(): Int64 {
    match (1) {
        case <!TYPE_MISMATCH!>(k, v)<!> => 1
        case _ => <!INVALID_LOOP_CONTROL!>continue<!>
    }
    0
}

