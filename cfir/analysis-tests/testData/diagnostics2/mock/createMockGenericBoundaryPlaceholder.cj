// RUN_PIPELINE_TILL: FRONTEND
class Box<T> {}

func mockNestedGenericNotInTestMode(): Unit {
    let value = <!MOCK_NOT_IN_TEST_MODE!>createMock<!><Box<Int64>>()
    let _ = value
}
