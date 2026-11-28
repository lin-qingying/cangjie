package helper_s1_lexical

public interface Left<X> {}
public interface Right<X> {}

public class LexicalOwner<A> {
    public init() {}

    // A 留在类的 lexical scope；仅方法自有 T/U 在 helper 内复制并改名。
    public func select<T, U>(ambient: A, first: T, second: U,
        value!: T = first, other!: U = second): U
        where T <: Left<A>, U <: Right<T> {
        other
    }
}

public func outer<A>(ambient: A): A {
    let captured: A = ambient
    func local(seed: A, unused: Int64, first!: A = seed, second!: A = captured): A {
        second
    }
    local(ambient, 0)
}

// 泛型改名遍历必须进入默认 lambda 及其局部函数，而外部值仍绑定到本 helper 的前序参数。
public func nested<T, U>(first: T, second: U,
    result!: (T) -> U = { candidate: T =>
        func choose(x: T): U { return second }
        choose(candidate)
    }): (T) -> U {
    result
}
