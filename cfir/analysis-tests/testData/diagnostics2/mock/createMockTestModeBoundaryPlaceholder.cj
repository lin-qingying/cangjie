// RUN_PIPELINE_TILL: FRONTEND
func mockStructUnsupportedBeforeTestModeCheck(): Unit {
    let value = <!MOCK_UNSUPPORTED_TYPE!>createMock<!><String>()
    let _ = value
}
