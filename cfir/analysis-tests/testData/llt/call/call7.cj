// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Day<T> {}

open class A <: I1 {}

open class B <: A & I2 {}

open class C <: B & I1 & I3 {}

open class D <: C {}

interface I1 {}

interface I2 <: I1 {}

interface I3 {}

func f1<X>(a: Day<X>, b: X, c: (X) -> Int64): Int64 {
    1
}

func f1<Y>(a: Day<Y>, b: Y, c: (B) -> Int64): Int64 where Y <: B {
    2
}

func f2<X>(a: Day<X>, b: X, c: (X) -> Int64): Int64 {
    3
}

func f2<Y>(a: Day<Y>, b: Y, c: (B) -> Int64): Int64 where Y <: B & I3 {
    4
}

main() {
    /* 候选集：f1, f2, 单独看对应位置参数，f2是最小的
      候选集中存在f1有公共类型变元，根据公共类型变元规则来判断f2是否满足
      f2中，Day<Y>中Y是不变，B是逆变，B是f2中Y上界B的父类，f2满足要求 */
    if (f1(Day<B>(), B(), {d: A => 0}) != 2) {
        return 1
    }
    /* 候选集：f1, f2, 单独看对应位置参数，f2是最小的
    候选集中存在f1有公共类型变元，根据公共类型变元规则来判断f2是否满足
    f2中，Day<Y>中Y是不变，B是逆变，B是f2中Y上界B的父类，f2满足要求 */
    if (f2(Day<C>(), C(), {d: A => 1}) != 4) {
        return 1
    }
    return 0
}
