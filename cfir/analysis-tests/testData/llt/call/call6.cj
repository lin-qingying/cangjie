// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Day<T> {}

open class A {}

open class B <: A & I2 {}

open class C <: B & I1 {}

open class D <: C {}

interface I1 {}

interface I2 <: I1 {}

func f<X>(a: Day<X>, b: X, c: (X) -> Unit): Int64 {
    1
} //f1

func f(a: Day<B>, b: C, c: (A) -> Unit): Int64 {
    2
} //f2

func f(a: Day<B>, b: I1, c: (D) -> Unit): Int64 {
    3
} //f3

func f<X, Y, Z>(a: X, b: Y, c: Z): Int64 {
    4
} //f4

main() {
    /* 候选集：f1, f2, f3, f4, 单独看对应位置参数，f2是最小的
    候选集中存在f1有公共类型变元，根据公共类型变元规则来判断f2是否满足
    f2中，B是不变，C是协变，C是B的子类型，A是逆变，A是B的父类型， */
    if (f(Day<B>(), D(), {d: A =>}) != 2) {
        return 1
    }
    return 0
}
