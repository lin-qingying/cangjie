// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
enum Option<T> {
    Some(T) | None
}

func mockEnumGenericUnsupported(): Unit {
    let value = <!MOCK_UNSUPPORTED_TYPE!>/* suggested message: enum-generic targets are not supported by the current mocking semantics. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */createMock<Option<Int64>>()<!>
    println(value)
}
