// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
func testOptionalChain(x: ?Box): Unit {
    let a = x?.value
    let b = x?.value?[0]
    x?.invoke?()
}
