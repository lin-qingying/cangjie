// RUN_PIPELINE_TILL: FRONTEND
func mockPrimitiveUnsupported(): Unit {
    let x = <!MOCK_UNSUPPORTED_TYPE!>createMock<!><Int64>()
    let _ = x
}

class MockableClass {}

func mockClassNotInTestMode(): Unit {
    let x = <!MOCK_NOT_IN_TEST_MODE!>createMock<!><MockableClass>()
    let _ = x
}
