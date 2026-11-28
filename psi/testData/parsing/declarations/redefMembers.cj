// redef 成员：子类对父类静态成员的重新定义与实例成员的 override。
// 官方依据：Cangjie 文档《继承》《重定义》；cjc 1.0.5 / 1.1.3 零诊断。
open class Base {
    public init() {}

    public static func s(): Int64 {
        return 1
    }

    public static prop p: Int64 {
        get() {
            return 2
        }
    }

    public open func f(): Int64 {
        return 3
    }
}

class Derived <: Base {
    public init() {
        super()
    }

    public redef static func s(): Int64 {
        return 10
    }

    public redef static prop p: Int64 {
        get() {
            return 20
        }
    }

    public override func f(): Int64 {
        return 30
    }
}
