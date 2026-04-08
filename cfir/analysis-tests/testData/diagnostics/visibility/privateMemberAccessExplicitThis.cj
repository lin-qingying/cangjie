// RUN_PIPELINE_TILL: FRONTEND
// 可见性检查测试：private 成员通过显式 this 在同类内部可访问
class Foo {
    private func secret(): Int64 {
        return 42
    }

    public func visible(): Int64 {
        return this.secret()
    }
}
