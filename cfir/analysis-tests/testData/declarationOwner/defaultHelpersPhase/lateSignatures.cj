package helper_phase_late_signatures

public func apply<A, R>(seed: A, action: (A) -> R): R {
    action(seed)
}

// 返回注解和 lambda 参数注解都省略；外层最终提交应包含全部嵌套声明。
public func lateSignature(seed: Int64) {
    func lateLocal() {
        apply(seed, { value =>
            func copy() { value }
            let inner: (Int64) -> Int64 = { extra => copy() + extra }
            inner(1)
        })
    }
    lateLocal()
}
