// RUN_PIPELINE_TILL: FRONTEND
class Foo {
    private func secret(): Int64 {
        return 42
    }

    public func visible(): Int64 {
        return this.secret()
    }
}
