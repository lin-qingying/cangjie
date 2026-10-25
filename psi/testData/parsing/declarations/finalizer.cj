// 终结器：~init() 在类中的声明与使用。
// 官方依据：Cangjie 文档《终结器》；cjc 1.0.5 / 1.1.3 零诊断。
class WithFinalizer {
    var v: Int64 = 0

    ~init() {
        this.v = 0
    }
}

class Resource {
    private var handle: Int64 = 1

    public init() {}

    ~init() {
        this.handle = 0
    }
}

