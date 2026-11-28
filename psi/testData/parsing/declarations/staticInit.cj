// 静态初始化与终结器：static init、static prop、static const 与 ~init 共存。
// 官方依据：Cangjie 文档《静态成员》《终结器》；cjc 1.0.5 / 1.1.3 零诊断。
class WithStaticInit {
    static var counter: Int64 = 0
    static const LIMIT: Int64 = 10
    var instance: Int64 = 0

    static prop current: Int64 {
        get() {
            return counter
        }
    }

    init() {
        instance = 1
    }

    static init() {
        counter = LIMIT
    }

    ~init() {
        counter = 0
    }
}

class Statics {
    static const A: Int64 = 1
    static const B: String = "b"
    static var v: Int64 = 0

    static mut prop p: Int64 {
        get() {
            return v
        }
        set(x) {
            v = x
        }
    }

    public init() {}
}
