// RUN_PIPELINE_TILL: FRONTEND
func classifyScore(score: Int64): Int64 {
    return match {
        case score < 60 => 0
        case score < 90 => <!UNRESOLVED_REFERENCE!>missingScore<!>
        case _ => 100
    }
}
