// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
func testSubscript(arr: Array<Int64>): Int64 {
    let first = arr[0]
    let second = arr[1]
    return first
}

