// RUN_PIPELINE_TILL: FRONTEND
// 可见性检查测试：private 成员跨文件不可访问
class Foo {


    private func secret(): Int64 {
        return 42
    }

    public func visible(): Int64 {
        return secret()  // OK: 同类访问
    }
}
