

open class A {}

open class B <: A {}

open class C <: B {}

open class D <: C {}

func g<X>(b: X, c: (Int64) -> X): Int64 {
    1
} // g1

func g<Y>(b: Y, c: (Int64) -> A): Int64 {
    2
} // g2

func f<X>(b: X, c: (X) -> Int64): Int64 {
    3
} // f1

func f<Y>(b: Y, c: (A) -> Int64): Int64 {
    4
} // f2

func h<X>(b: X, c: (X) -> Int64): Int64 {
    5
} // h1

func h<Y>(b: A, c: (Y) -> Int64): Int64 where Y <: C {
    6
} // h2

main() {
    /* 候选集：g1, g2, 单独看对应位置参数，g2是最匹配
       候选集中存在g1有公共类型变元，根据公共类型变元规则来判断g2是否满足
       g2中，Y是协变，A是协变，Y无类型约束，g2不满足要求 */
    <!AMBIGUOUS_FUNCTION_CALL!>g<!>(C(), {a: Int64 => A()})

    /* 候选集：g1, g2, 单独看对应位置参数，g2是最匹配
       候选集中存在g1有公共类型变元，根据公共类型变元规则来判断g2是否满足
       g2中，Y是协变，A是逆变，Y无类型约束，g2不满足要求 */
    <!AMBIGUOUS_FUNCTION_CALL!>f<!>(C(), {d: A => Int64(0)})

    /* 候选集：g1, g2, 单独看对应位置参数，g2是最匹配
       候选集中存在g1有公共类型变元，根据公共类型变元规则来判断g2是否满足
       g2中，Y是逆变，A是协变，A无法是Y的严格子类型，因此无法决议 */
    <!AMBIGUOUS_FUNCTION_CALL!>h<!>(C(), {d: C => Int64(0)})
    return 0
}
