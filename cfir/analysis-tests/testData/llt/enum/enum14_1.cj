// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum TimeUnit1 {
    Year(Int32) | Day(TimeUnit1)
}

enum TimeUnit1_2 {
    Year(Int64) | Day(TimeUnit1_2)
}

class A {}

class B {}

func f(a: A) {
    a
}

func f(a: B) {
    a
}

func g(a: Int64): A {
    A()
}

func g(a: Int32): B {
    B()
}

main(): Int64 {
    var a = Day(Year(1))
    f(g(1))
    return 0
}

/* SCAN
error: find multiple constructor 'Day' of enum declaration
  ==> enum14_1.cj:38:13:
   | 
38 |     var a = Day(Year(1))
   |             ^ 
   | 
note: found candidate
  ==> enum14_1.cj:10:19:
   | 
10 |     Year(Int32) | Day(TimeUnit1)
   |                   ^ 
   | 
note: found candidate
  ==> enum14_1.cj:14:19:
   | 
14 |     Year(Int64) | Day(TimeUnit1_2)
   |                   ^ 
   | 

error: ambiguous match for function call 'f'
  ==> enum14_1.cj:39:5:
   | 
39 |     f(g(1))
   |     ^ 
   | 
note: found candidate
  ==> enum14_1.cj:21:6:
   | 
21 | func f(a: A) {
   |      ^ 
   | 
note: found candidate
  ==> enum14_1.cj:25:6:
   | 
25 | func f(a: B) {
   |      ^ 
   | 

2 errors generated, 2 errors printed.
*/
