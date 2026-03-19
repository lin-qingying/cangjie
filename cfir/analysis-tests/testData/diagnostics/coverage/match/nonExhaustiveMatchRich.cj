// RUN_PIPELINE_TILL: FRONTEND
func boolCase(b: Bool): Int64 {
    return <!NON_EXHAUSTIVE_MATCH!>match (b) {
        case true => 1
    }<!>
}

func intCase(x: Int64): Int64 {
    return <!NON_EXHAUSTIVE_MATCH!>match (x) {
        case 0 => 0
        case 1 => 1
    }<!>
}

func tupleCase(a: (Int64, Int64)): Int64 {
    return <!NON_EXHAUSTIVE_MATCH!>match (a) {
        case (0, 0) => 0
    }<!>
}

