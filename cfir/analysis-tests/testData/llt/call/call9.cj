// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A {}

open class B <: A {}

open class C <: B {}

open class D <: C {}

func g<X>(b: X, c: (Int64) -> X): Int64 {
    1
} // g1

func g<Y>(b: Y, c: (Int64) -> A): Int64 where Y <: C {
    2
} // g2

func f<X>(b: X, c: (X) -> Int64): Int64 {
    3
} // f1

func f<Y>(b: Y, c: (A) -> Int64): Int64 where Y <: C {
    4
} // f2

func h<X>(b: (X) -> Int64, c: (X) -> Int64): Int64 where X <: B {
    5
} // h1

func h<Y>(b: (B) -> Int64, c: (Y) -> Int64): Int64 where Y <: C {
    6
} // h2

main() {
    /* 候选集：g1, g2, 单独看对应位置参数，g2是最匹配
      候选集中存在g1有公共类型变元，根据公共类型变元规则来判断g2是否满足
      g2中，Y是协变，A是协变，A是g2中Y上界C的父类，g2满足要求 */
    if (g(C(), {a: Int64 => A()}) != 2) {
        return 1
    }
    /* 候选集：f1, f2, 单独看对应位置参数，g2是最匹配
       候选集中存在f1有公共类型变元，根据公共类型变元规则来判断g2是否满足
       f2中，Y是协变，A是逆变，A是f2中Y上界C的父类，f2满足要求 */
    if (f(C(), {d: A => 0}) != 4) {
        return 4
    }
    /* 候选集：h1, h2, 单独看对应位置参数，h2是最匹配
       候选集中存在h1有公共类型变元，根据公共类型变元规则来判断h2是否满足
       h2中，B是逆变，Y是逆变，Y是h1中X的子类，h2满足要求 */
    if (h({a: A => 1}, {b: C => 2}) != 6) {
        return 6
    }
    /*
     * h1 forwards to h2: solve Y
     * B <: X, Y <: X. false
     * h2 forwards to h1: solve X
     * X <: B & Y.   Y <: C
     * X <: B & Y
     * X <: Y
     */
    return 0
}
