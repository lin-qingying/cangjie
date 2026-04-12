// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

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
    g(C(), {a: Int64 => A()})

    /* 候选集：g1, g2, 单独看对应位置参数，g2是最匹配
       候选集中存在g1有公共类型变元，根据公共类型变元规则来判断g2是否满足
       g2中，Y是协变，A是逆变，Y无类型约束，g2不满足要求 */
    f(C(), {d: A => Int64(0)})

    /* 候选集：g1, g2, 单独看对应位置参数，g2是最匹配
       候选集中存在g1有公共类型变元，根据公共类型变元规则来判断g2是否满足
       g2中，Y是逆变，A是协变，A无法是Y的严格子类型，因此无法决议 */
    h(C(), {d: C => Int64(0)})
    return 0
}

/* SCAN
error: ambiguous match for function call 'g'
  ==> call10.cj:45:5:
   | 
45 |     g(C(), {a: Int64 => A()})
   |     ^ 
   | 
note: found candidate
  ==> call10.cj:17:6:
   | 
17 | func g<X>(b: X, c: (Int64) -> X): Int64 {
   |      ^ 
   | 
note: found candidate
  ==> call10.cj:21:6:
   | 
21 | func g<Y>(b: Y, c: (Int64) -> A): Int64 {
   |      ^ 
   | 

error: ambiguous match for function call 'f'
  ==> call10.cj:50:5:
   | 
50 |     f(C(), {d: A => Int64(0)})
   |     ^ 
   | 
note: found candidate
  ==> call10.cj:25:6:
   | 
25 | func f<X>(b: X, c: (X) -> Int64): Int64 {
   |      ^ 
   | 
note: found candidate
  ==> call10.cj:29:6:
   | 
29 | func f<Y>(b: Y, c: (A) -> Int64): Int64 {
   |      ^ 
   | 

error: ambiguous match for function call 'h'
  ==> call10.cj:55:5:
   | 
55 |     h(C(), {d: C => Int64(0)})
   |     ^ 
   | 
note: found candidate
  ==> call10.cj:33:6:
   | 
33 | func h<X>(b: X, c: (X) -> Int64): Int64 {
   |      ^ 
   | 
note: found candidate
  ==> call10.cj:37:6:
   | 
37 | func h<Y>(b: A, c: (Y) -> Int64): Int64 where Y <: C {
   |      ^ 
   | 

3 errors generated, 3 errors printed.
*/
