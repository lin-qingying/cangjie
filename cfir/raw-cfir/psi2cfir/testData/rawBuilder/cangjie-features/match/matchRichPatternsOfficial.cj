// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
func classifyScore(score: Int64): String {
    return match {
        case score < 60 => "fail"
        case score < 70 => "pass"
        case score < 90 => "good"
        case _ => "excellent"
    }
}

func extractYear(time: TimeUnit): Int32 {
    return match (time) {
        case Year(y)  where 1 == 1 => y
        case Year(y) | Month(y, _) => y
    }
}
