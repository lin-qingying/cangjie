// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
func mockNotInTestMode(): Unit {
    let value = <!MOCK_NOT_IN_TEST_MODE!>/* suggested message: mocking APIs may only be used in dedicated test mode. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */createMock<String>()<!>
    println(value)
}
