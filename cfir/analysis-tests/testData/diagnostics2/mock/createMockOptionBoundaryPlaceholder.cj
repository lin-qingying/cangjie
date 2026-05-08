// RUN_PIPELINE_TILL: FRONTEND
enum Option<T> {
    Some(T) | None
}

func mockEnumUnsupported(): Unit {
    let value = <!MOCK_UNSUPPORTED_TYPE!>createMock<!><Option<Int64>>()
    let _ = value
}
