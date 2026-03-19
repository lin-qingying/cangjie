// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
func localScopeInNestedBlock(): Unit {
    if (true) {
        func nestedLocal(): Unit {}
        let nestedValue: Int64 = 1
    }
}

