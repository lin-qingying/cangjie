// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
func safeDivide(a: Int64, b: Int64): Int64 {
    try {
        return a / b
    } catch (e: Exception) {
        return 0
    } finally {
        let done = true
    }
}

func tryCatchOnly(x: Int64): Int64 {
    try {
        return x
    } catch (e: Exception) {
        return 0
    }
}

