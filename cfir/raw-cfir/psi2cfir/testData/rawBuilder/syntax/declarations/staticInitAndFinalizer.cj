// RUN_PIPELINE_TILL: FRONTEND
// 静态初始化器 static init()、~init() 析构函数、static 成员、static const、static prop；同时存在实例 init 与 static init。
// 官方依据：Cangjie 文档《静态初始化器》《析构函数》；cjc 1.0.5 / 1.1.3 零诊断。
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
        instance = 0
    }
}

class OnlyFinalizer {
    var v: Int64 = 0

    ~init() {
        v = 0
    }
}
