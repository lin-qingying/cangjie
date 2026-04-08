// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func mockDisabled(): Unit {
    let x = <!MOCK_DISABLED!>/* suggested message: mocking is not enabled in the current compilation or test mode. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */createMock<Int64>()<!>
    println(x)
}

func mockUnsupportedType(): Unit {
    let x = <!MOCK_UNSUPPORTED_TYPE!>/* suggested message: requested mock target type is not supported by the mocking semantics. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */createMock<Int64>()<!>
    println(x)
}
