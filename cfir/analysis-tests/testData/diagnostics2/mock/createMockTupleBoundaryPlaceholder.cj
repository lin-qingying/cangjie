// RUN_PIPELINE_TILL: FRONTEND
func mockTupleUnsupported(): Unit {
    let value = <!MOCK_UNSUPPORTED_TYPE!>createMock<(Int64, Int64)>()<!>
    let _ = value
}
