// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class Box<T> {}

func mockNestedGenericUnsupported(): Unit {
    let value = <!MOCK_UNSUPPORTED_TYPE!>/* suggested message: nested generic mock targets are not supported by the mocking semantics. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */createMock<Box<Int64>>()<!>
    println(value)
}
