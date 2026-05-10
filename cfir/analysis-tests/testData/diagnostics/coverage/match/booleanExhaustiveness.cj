// RUN_PIPELINE_TILL: FRONTEND
func exhaustiveBool(b: Bool): Int64 {
    return match (b) {
        case true => 1
        case false => 0
    }
}

func missingFalse(b: Bool): Int64 {
    return match (<!NON_EXHAUSTIVE_MATCH!>b<!>) {
        case true => 1
    }
}

func missingTrue(b: Bool): Int64 {
    return match (<!NON_EXHAUSTIVE_MATCH!>b<!>) {
        case false => 0
    }
}
