// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func mockFunctionUnsupported(): Unit {
    let value = <!MOCK_UNSUPPORTED_TYPE!>/* suggested message: function types are not supported as mock targets in the current mocking semantics. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */createMock<(Int64) -> Int64>()<!>
    println(value)
}
