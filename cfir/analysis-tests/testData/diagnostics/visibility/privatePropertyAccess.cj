// RUN_PIPELINE_TILL: FRONTEND
// 可见性检查测试：private 属性在同类内部可访问
class Counter {
    private let secret: Int64 = 42

    public func visible(): Int64 {
        return secret
    }
}
