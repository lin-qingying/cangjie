// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func mockTupleUnsupported(): Unit {
    let value = <!MOCK_UNSUPPORTED_TYPE!>/* suggested message: tuple-shaped targets are not supported by the current mocking semantics. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */createMock<(Int64, Int64)>()<!>
    println(value)
}
