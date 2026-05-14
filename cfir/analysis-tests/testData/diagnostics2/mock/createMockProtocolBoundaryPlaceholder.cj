// RUN_PIPELINE_TILL: FRONTEND
interface Service {
    func call(): Unit
}

func mockProtocolNotInTestMode(): Unit {
    let value = <!MOCK_NOT_IN_TEST_MODE!>createMock<Service>()<!>
    let _ = value
}
