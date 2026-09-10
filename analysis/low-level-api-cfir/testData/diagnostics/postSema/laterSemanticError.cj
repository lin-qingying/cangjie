package test

func laterSemanticError(): Int64 {
    match (1) {
        case 0 => 0
        case _ => missingValue
    }
}
