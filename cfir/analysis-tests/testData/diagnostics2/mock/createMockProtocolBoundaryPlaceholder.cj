// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
interface Service {
    func call(): Unit
}

func mockProtocolUnsupported(): Unit {
    let value = <!MOCK_UNSUPPORTED_TYPE!>/* suggested message: protocol-like abstraction targets are not supported by the current mocking semantics. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */createMock<Service>()<!>
    println(value)
}
