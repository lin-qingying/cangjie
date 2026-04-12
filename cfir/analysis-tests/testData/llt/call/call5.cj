// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class Day<T> {}

open class A {}

open class B <: A & I1 {}

open class C <: B & I2 {}

open class D <: C {}

interface I1 {}

interface I2 <: I1 {}

func f<X>(a: Day<X>, b: X, c: (X) -> Unit): Int64 {
    1
} //f1

func f(a: Day<B>, b: I2, c: (C) -> Unit): Int64 {
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
    f2中，B是不变，I2是协变，I2不是B的子类型，因此f2不满足公共类型变元的规则。*/
    f(Day<B>(), C(), {d: A =>})
    return 0
}

/* SCAN
error: ambiguous match for function call 'f'
  ==> call5.cj:43:5:
   | 
43 |     f(Day<B>(), C(), {d: A =>})
   |     ^ 
   | 
note: found candidate
  ==> call5.cj:23:6:
   | 
23 | func f<X>(a: Day<X>, b: X, c: (X) -> Unit): Int64 {
   |      ^ 
   | 
note: found candidate
  ==> call5.cj:27:6:
   | 
27 | func f(a: Day<B>, b: I2, c: (C) -> Unit): Int64 {
   |      ^ 
   | 
note: found candidate
  ==> call5.cj:31:6:
   | 
31 | func f(a: Day<B>, b: I1, c: (D) -> Unit): Int64 {
   |      ^ 
   | 
note: found candidate
  ==> call5.cj:35:6:
   | 
35 | func f<X, Y, Z>(a: X, b: Y, c: Z): Int64 {
   |      ^ 
   | 

1 error generated, 1 error printed.
*/
