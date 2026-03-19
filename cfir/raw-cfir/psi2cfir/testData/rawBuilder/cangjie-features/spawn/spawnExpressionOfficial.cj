// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
main(): Int64 {
    let i: Int64 = 0
    let f1: Future<Int64> = spawn {
        => return i
    }
    let f2 = spawn {
        => return i
    }
    return i
}
