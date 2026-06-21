

class Day<T> {}

open class A <: I1 {}

open class B <: A & I2 {}

open class C <: B & I1 & I3 {}

open class D <: C {}

interface I1 {}

interface I2 <: I1 {}

interface I3 {}

func f<X>(a: Day<X>, b: X, c: (X) -> Int64): Int64 {
    0
} //f1

func f<Y>(a: Day<Y>, b: Y, c: (B) -> Int64): Int64 {
    1
} //f2

main() {
    /* 候选集：f1, f2, 单独看对应位置参数，f2是最小的
    候选集中存在f1有公共类型变元，根据公共类型变元规则来判断f2是否满足
    f2中，Day<Y>中Y是不变，B是逆变，Y无约束上界，f2不满足要求 */
    <!AMBIGUOUS_FUNCTION_CALL!>f<!>(Day<B>(), B(), {d: A => 0})
}
