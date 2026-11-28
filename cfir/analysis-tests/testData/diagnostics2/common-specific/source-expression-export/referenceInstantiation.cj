package source_reference_instantiation

// 对照普通成员、成员函数值与泛型函数；父类型状态由官方匹配产物确定。
public open class PlainParent {
    public let tag: Int64 = 1

    public func plain(value: Int64): Int64 {
        value + 1
    }

    public func generic<U>(value: U): U {
        value
    }
}

// 普通用户定义泛型结构，不依赖 Array 或下标操作符。
public struct GenericCarrier<T> {
    public let payload: T

    public init(value: T) {
        payload = value
    }

    public func echo(value: T): T {
        value
    }

    public func constant(value: Int64): Int64 {
        value + 2
    }
}

public func ordinary(value: Int64): Int64 {
    value + 5
}

public func identity<T>(value: T): T {
    value
}

public interface PlainContract {
    func accepted(value: Int64): Int64 {
        value + 3
    }
}

public interface GenericContract<T> {
    func supplied(value: T): T {
        value
    }

    func fixed(value: Int64): Int64 {
        value + 4
    }
}

public class ContractImplementation <: GenericContract<Int64> & PlainContract {}

// 所有回归入口都是接口默认正文，要求真实非零 SOURCE 正文及完整引用边。
public interface ReferenceInstantiation {
    func nonGeneric(receiver: PlainParent, value: Int64): Int64 {
        let callable: (Int64) -> Int64 = receiver.plain
        return ordinary(value) + receiver.tag + receiver.plain(value) + callable(value)
    }

    func genericStructure(receiver: GenericCarrier<Int64>, value: Int64): Int64 {
        let callable: (Int64) -> Int64 = receiver.echo
        return receiver.payload + receiver.echo(value) + receiver.constant(value) + callable(value)
    }

    func genericCalls(receiver: PlainParent, value: Int64): Int64 {
        let callable: (Int64) -> Int64 = identity<Int64>
        return identity<Int64>(value) + identity(value) + receiver.generic<Int64>(value) + callable(value)
    }

    func interfaceMatch(receiver: PlainContract, value: Int64): Int64 {
        return receiver.accepted(value)
    }

    func genericInterfaceMatch(receiver: GenericContract<Int64>, value: Int64): Int64 {
        return receiver.supplied(value) + receiver.fixed(value)
    }

    func concreteInterfaceMatch(receiver: ContractImplementation, value: Int64): Int64 {
        return receiver.supplied(value) + receiver.fixed(value) + receiver.accepted(value)
    }

    func upperBound<T>(receiver: T, value: Int64): Int64 where T <: PlainContract {
        return receiver.accepted(value)
    }

    func genericUpperBound<T, U>(receiver: T, value: U): U where T <: GenericContract<U> {
        return receiver.supplied(value)
    }

    func concreteGenericUpperBound<T>(receiver: T, value: Int64): Int64 where T <: GenericContract<Int64> {
        return receiver.supplied(value) + receiver.fixed(value)
    }

    func classUpperBound<T>(receiver: T, value: Int64): Int64 where T <: PlainParent {
        return receiver.plain(value)
    }
}
