package helper_s1_own_generics

public interface Left<X> {}
public interface Marker {}
public interface Right<X> {}

// 两个自有泛型、交叉上界实参及多重上界均属于各 helper 独立的声明域。
public func bounded<T, U>(first: T, second: U,
    value!: T = first, other!: U = second): U
    where T <: Left<U> & Marker, U <: Right<T> {
    other
}
