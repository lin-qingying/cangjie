// 构造器委托：this(...) 同类委托与 super(...) 父类委托。
// 官方依据：Cangjie 文档《构造函数》；cjc 1.0.5 / 1.1.3 零诊断。
open class DelegBase {
    public var v: Int64 = 0

    public init(x: Int64) {
        this.v = x
    }

    public init() {
        this.v = 0
    }
}

class DelegDerived <: DelegBase {
    var w: Int64 = 0

    init() {
        super(1)
        this.w = 2
    }

    init(y: Int64) {
        this()
        this.w = y
    }

    init(y: Int64, z!: Int64) {
        this(y)
        this.w = z
    }
}
