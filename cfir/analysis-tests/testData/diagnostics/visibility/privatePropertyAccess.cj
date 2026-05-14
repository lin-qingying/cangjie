// RUN_PIPELINE_TILL: FRONTEND
class Counter {
    private let secret: Int64 = 42

    public func visible(): Int64 {
        return secret
    }
}
