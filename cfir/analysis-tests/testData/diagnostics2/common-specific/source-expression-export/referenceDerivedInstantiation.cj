package source_reference_derived_instantiation

// 原始成员、所属父类及成员签名都没有类型参数。
public open class PlainParent {
    public func plain(value: Int64): Int64 {
        value + 1
    }
}

// 类型参数仅属于派生接收者，不出现在继承成员的签名中。
public open class Derived<T> <: PlainParent {}

// 使用接口默认正文，保留官方 SOURCE 导出的真实调用与函数值引用图。
public interface ReferenceDerivedInstantiation {
    func derived(receiver: Derived<Int64>, value: Int64): Int64 {
        let callable: (Int64) -> Int64 = receiver.plain
        return receiver.plain(value) + callable(value)
    }

    func parent(receiver: PlainParent, value: Int64): Int64 {
        let callable: (Int64) -> Int64 = receiver.plain
        return receiver.plain(value) + callable(value)
    }

    func derivedUpperBound<T>(receiver: T, value: Int64): Int64 where T <: Derived<Int64> {
        let callable: (Int64) -> Int64 = receiver.plain
        return receiver.plain(value) + callable(value)
    }

    func parentUpperBound<T>(receiver: T, value: Int64): Int64 where T <: PlainParent {
        let callable: (Int64) -> Int64 = receiver.plain
        return receiver.plain(value) + callable(value)
    }

    func genericDerived<U>(receiver: Derived<U>, value: Int64): Int64 {
        let callable: (Int64) -> Int64 = receiver.plain
        return receiver.plain(value) + callable(value)
    }

    func genericDerivedUpperBound<T, U>(receiver: T, value: Int64): Int64 where T <: Derived<U> {
        let callable: (Int64) -> Int64 = receiver.plain
        return receiver.plain(value) + callable(value)
    }
}
