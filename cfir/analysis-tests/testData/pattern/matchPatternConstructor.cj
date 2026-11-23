// RUN_PIPELINE_TILL: FRONTEND
func probeA(): ?UInt64 {
    match (Some(1)) {
        case None1 => return None
        case <!UNREACHABLE_PATTERN!>Some(v)<!> => 1
    }
}

func probeB(): ?UInt64 {
    match (Some(1)) {
        case None => return None
        case <!UNRESOLVED_REFERENCE!>Some1<!>(v) => 1
    }
}

func probeC(): ?UInt64 {
    match (Some(1)) {
        case None => return None
        case <!UNRESOLVED_REFERENCE!>Som<!>(v) => 1
    }
}

main(): Int64 {
    return 0
}
