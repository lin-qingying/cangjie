// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %compile_lib_opt %f -o %output 2>&1 | compare %f

class ConsTest {
    public static func f(a: Bool): Unit {}
    public static func f(): Unit {}
}

func g(a: Bool): Unit {}

func g(): Unit {}

let a: (Bool) -> Unit = g // OK
let b: () -> Unit = ConsTest.f // OK
let c: (Bool) -> Unit = ConsTest.f // OK
let d = ConsTest.f
let lst = [(ConsTest.f, 1), (ConsTest.f, 2)]

main(): Int64 {
    let x = lst[0][0]
    x + 1
    0
}

/* SCAN
error: ambiguous use of 'f'
  ==> overload_resolution_1.cj:21:9:
   | 
21 | let d = ConsTest.f
   |         ^^^^^^^^^^ 
   | 
note: found candidate
  ==> overload_resolution_1.cj:10:24:
   | 
10 |     public static func f(a: Bool): Unit {}
   |                        ^ 
   | 
note: found candidate
  ==> overload_resolution_1.cj:11:24:
   | 
11 |     public static func f(): Unit {}
   |                        ^ 
   | 

error: ambiguous use of 'f'
  ==> overload_resolution_1.cj:22:13:
   | 
22 | let lst = [(ConsTest.f, 1), (ConsTest.f, 2)]
   |             ^^^^^^^^^^ 
   | 
note: found candidate
  ==> overload_resolution_1.cj:10:24:
   | 
10 |     public static func f(a: Bool): Unit {}
   |                        ^ 
   | 
note: found candidate
  ==> overload_resolution_1.cj:11:24:
   | 
11 |     public static func f(): Unit {}
   |                        ^ 
   | 

error: ambiguous use of 'f'
  ==> overload_resolution_1.cj:22:30:
   | 
22 | let lst = [(ConsTest.f, 1), (ConsTest.f, 2)]
   |                              ^^^^^^^^^^ 
   | 
note: found candidate
  ==> overload_resolution_1.cj:10:24:
   | 
10 |     public static func f(a: Bool): Unit {}
   |                        ^ 
   | 
note: found candidate
  ==> overload_resolution_1.cj:11:24:
   | 
11 |     public static func f(): Unit {}
   |                        ^ 
   | 

3 errors generated, 3 errors printed.
*/
