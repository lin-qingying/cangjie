// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
enum TimeUnit {
    | Year(Int32)
}

func extractYear(time: TimeUnit): Int32 {
    var res: Int32 = match (time) {
        case Year(x) => x
    }
    return res
}
