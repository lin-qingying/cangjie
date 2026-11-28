package s4_frozen_type_roles

// Alias 声明自身、RHS 和使用槽分别保留其类型角色。
public type Pair<T> = (T, T)
public type PairLink<T> = Pair<T>
public type IntPair = PairLink<Int64>
public type IntPairChain = IntPair

// self type 的实参绑定本类自己的 T、U；This 独立保留标记。
public class Carrier<T, U> {
    public func asThis(): This {
        return this
    }

    public func asNominal(): Carrier<T, U> {
        return this
    }

    public func choose(first: T, middle: U, last: T): U {
        return middle
    }

    public func pair(value: T): Pair<T> {
        return (value, value)
    }
}

// 同名 T 属于函数，与 Alias、class 的 T 分别绑定。
public func pairOf<T>(value: T): Pair<T> {
    return (value, value)
}

public func orderedSlots(first: Int64, middle: Bool, last: Int64): Bool {
    return middle
}

public func reorderedSlots(first: Bool, middle: Int64, last: Int64): Bool {
    return first
}

public func repeatedReturn(first: Int64, last: Int64): Int64 {
    return first
}

// 接口默认正文提供真实 SOURCE 使用路径。
public interface FrozenTypeSource {
    func aliasUse(value: IntPairChain): IntPairChain {
        return value
    }

    func tupleUse(value: (Int64, Int64)): (Int64, Int64) {
        return value
    }

    func functionUse(
        callback: (Int64, Bool, Int64) -> Bool,
        first: Int64,
        middle: Bool,
        last: Int64
    ): Bool {
        return callback(first, middle, last)
    }

    func functionReturn(
        callback: (Int64, Bool, Int64) -> Bool
    ): (Int64, Bool, Int64) -> Bool {
        return callback
    }

    func carrierUse(value: Carrier<Int64, Bool>): Carrier<Int64, Bool> {
        return value.asThis()
    }

    func publicArrayUse(
        value: Array<Array<Int64>>
    ): Array<Array<Int64>> {
        return value
    }
}
