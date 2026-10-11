// RUN_PIPELINE_TILL: FRONTEND
// this / super 限定调用：子类实例方法里 this.f() 调用继承的 open 方法、super.f() 调用父类实现。
// 官方依据：Cangjie 文档《成员函数》《继承》；cjc 1.0.5 / 1.1.3 零诊断（super 单独出现报 invalid super expression，见 recovery）。
open class Base {
    public init() {}

    public open func f(): Int64 {
        return 1
    }
}

class Derived <: Base {
    public init() {
        super()
    }

    public func viaThis(): Int64 {
        return this.f()
    }

    public func viaSuper(): Int64 {
        return super.f()
    }
}

func thisSuperQualified(): Int64 {
    return Derived().viaThis() + Derived().viaSuper()
}
