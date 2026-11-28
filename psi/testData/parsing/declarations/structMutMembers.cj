// struct 可变成员：mut prop 与 mut func（值语义下修改自身的成员）。
// 官方依据：Cangjie 文档《struct 类型》《mut》；cjc 1.0.5 / 1.1.3 零诊断。
struct MutStructHolder {
    private var state: Int64 = 0

    public init() {}

    mut prop value: Int64 {
        get() {
            return state
        }
        set(v) {
            state = v
        }
    }

    mut func reset(): Unit {
        state = 0
    }

    public func read(): Int64 {
        return state
    }
}

struct CounterPair {
    var first: Int64 = 0
    var second: Int64 = 0

    public init() {}

    mut func swap(): Unit {
        let tmp = first
        first = second
        second = tmp
    }
}
