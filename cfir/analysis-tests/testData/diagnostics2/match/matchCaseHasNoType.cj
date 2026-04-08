// RUN_PIPELINE_TILL: FRONTEND
func classifyScore(score: Int64): Int64 {
    return <!RETURN_TYPE_MISMATCH!>match {
        case score < 60 => 0
        <!MATCH_CASE_HAS_NO_TYPE!>case score < 90 => <!UNRESOLVED_REFERENCE!>/* suggested message: unresolved reference: missingScore. source: existing unresolved diagnostics semantics in testData/diagnostics/unresolved plus current match-case no-type regression shape */missingScore<!><!>
        case _ => 100
    }<!>
}
