// RUN_PIPELINE_TILL: FRONTEND
func boolCase(b: Bool): Int64 {
    return match (<!NON_EXHAUSTIVE_MATCH!>b<!>) {
        case true => 1
    }
}

func intCase(x: Int64): Int64 {
    return match (<!NON_EXHAUSTIVE_MATCH!>x<!>) {
        case 0 => 0
        case 1 => 1
    }
}

func tupleCase(a: (Int64, Int64)): Int64 {
    return match (<!NON_EXHAUSTIVE_MATCH!>a<!>) {
        case (0, 0) => 0
    }
}
