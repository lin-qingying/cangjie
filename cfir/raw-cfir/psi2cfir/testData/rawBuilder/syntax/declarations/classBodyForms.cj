// RUN_PIPELINE_TILL: FRONTEND
// 类成员：字段（var / let / private / protected / internal / static）、static const、prop（只读 / mut get+set / 静态 prop）、
// 构造器（无参 / 具名形参 / this 委托（具名形参必须按名传）/ super 委托）、静态初始化器、~init 析构、成员函数（各种可见性 /
// static / open / override / redef（redef 只能修饰 static 成员）/ operator）。
// 官方依据：Cangjie 文档《class 定义》《属性》《构造函数》；cjc 1.0.5 / 1.1.3 零诊断（abstract prop 只能写在 interface，见 recovery/abstractPropInClass.cj）。
class Members {
    var instanceField: Int64 = 0
    let immutableField: String
    private var privateField: Int64 = 0
    protected var protectedField: Int64 = 0
    internal var internalField: Int64 = 0
    static var staticField: Int64 = 0
    static const STATIC_CONST: Int64 = 1

    prop readOnly: Int64 {
        get() {
            return instanceField
        }
    }

    mut prop readWrite: Int64 {
        get() {
            return instanceField
        }

        set(v) {
            instanceField = v
        }
    }

    static prop staticReadOnly: Int64 {
        get() {
            return staticField
        }
    }

    public init() {
        staticField = 1
        immutableField = "x"
    }

    public init(a: Int64, b!: String) {
        instanceField = a
        immutableField = b
    }

    public init(other: Members) {
        this(other.instanceField, b: other.immutableField)
    }

    public func method(): Int64 {
        return instanceField + privateField + protectedField + internalField
    }

    private func privateMethod(): Int64 {
        return 1
    }

    protected func protectedMethod(): Int64 {
        return 2
    }

    internal func internalMethod(): Int64 {
        return 3
    }

    public static func staticMethod(): Int64 {
        return staticField
    }

    public operator func +(other: Members): Members {
        return other
    }

    ~init() {
        instanceField = 0
    }
}

open class OpenBase {
    public init() {
    }

    public open func openFunc(): Int64 {
        return 1
    }

    public static func staticFunc(): Int64 {
        return 2
    }
}

class RedefDerived <: OpenBase {
    public init() {
        super()
    }

    public redef static func staticFunc(): Int64 {
        return 3
    }
}
