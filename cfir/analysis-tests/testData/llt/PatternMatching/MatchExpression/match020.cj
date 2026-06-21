

main() {
    CaseRunner<Int8>().runCases()
    CaseRunner<Int16>().runCases()
    return 0
}

class CaseRunner<T> where T <: Equatable<T> {
    public func runCases(checktype!: String = "AssertEquals") {
        match (checktype) {
            case "AssertNotEqual" => print("1")
            case "AssertEquals" => print("2")
            case _ => print("3")
        }
    }
}
