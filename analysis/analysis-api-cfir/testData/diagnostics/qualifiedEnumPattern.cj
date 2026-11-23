public enum Local {
    | A
    | B(Int64)
}

func qualifiedKnown(x: Local): Int64 {
    match (x) {
        case Local.A => 0
        case Local.B(v) => v
    }
}

func qualifiedUnknown(x: Local): Int64 {
    match (x) {
        case Local.A => 0
        case Local.Missing(v) => 1
    }
}

main(): Int64 {
    return 0
}
