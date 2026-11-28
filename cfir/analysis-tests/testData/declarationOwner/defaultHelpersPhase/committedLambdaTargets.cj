package helper_phase_committed_targets

public class NumberInput {
    public let value: Int64 = 3
    public init() {}
}

public class TextInput {
    public let value: String = "text"
    public init() {}
}

public func choose(action: (NumberInput) -> Int64): Int64 { action(NumberInput()) }
public func choose(action: (TextInput) -> Int64): Int64 { action(TextInput()) }

// 顶层语句的 Int64 约束使 TextInput 候选不匹配；嵌套函数保留隐式返回类型。
public func overloadCommitted() {
    choose({ item =>
        let selected: Int64 = item.value
        func lateLocal() { selected + 1 }
        let inner: (Int64) -> Int64 = { extra =>
            func add() { lateLocal() + extra }
            add()
        }
        inner(2)
    })
}

public func infer<A, R>(action: (A) -> R): (A) -> R { action }
public func asNumber(value: Int64): Int64 { value }

// A 无普通实参提供约束，必须由 lambda 正文调用反推，再写回最终参数和返回类型。
public func pclaCommitted() {
    let inferred = infer({ value =>
        func read() { asNumber(value) }
        let inner: (Int64) -> Int64 = { extra =>
            func add() { read() + extra }
            add()
        }
        inner(1)
    })
    inferred(2)
}
