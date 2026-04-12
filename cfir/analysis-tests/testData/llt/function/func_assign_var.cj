// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class Base {}

class Sub <: Base {
    public let f = {x: Sub => 3}
    public func ff(x: Sub, y!: Int32 = 0): Int32 {
        4
    }
    public func ff(x: Sub): Int32 {
        5
    }
}

func a(x: Base) {
    return Base()
}

func a(x: Sub): Int32 {
    return 2
}

func a(x: Int32, y: Int32): Int32 {
    return x + y
}

func codegenUnitTest(): Int64 {
    var x: (Int64) -> Int32 = a
    var b = Sub().ff
    var z = a
    return 0
}

main(): Int64 {
    return codegenUnitTest()
}

/* SCAN
error: no matching function declaration for function reference 'a'
  ==> func_assign_var.cj:34:31:
   | 
34 |     var x: (Int64) -> Int32 = a
   |                               ^ 
   | 

error: ambiguous use of 'ff'
  ==> func_assign_var.cj:35:13:
   | 
35 |     var b = Sub().ff
   |             ^^^^^^^^ 
   | 
note: found candidate
  ==> func_assign_var.cj:13:17:
   | 
13 |     public func ff(x: Sub, y!: Int32 = 0): Int32 {
   |                 ^^ 
   | 
note: found candidate
  ==> func_assign_var.cj:16:17:
   | 
16 |     public func ff(x: Sub): Int32 {
   |                 ^^ 
   | 

error: ambiguous use of 'a'
  ==> func_assign_var.cj:36:13:
   | 
36 |     var z = a
   |             ^ 
   | 
note: found candidate
  ==> func_assign_var.cj:21:6:
   | 
21 | func a(x: Base) {
   |      ^ 
   | 
note: found candidate
  ==> func_assign_var.cj:25:6:
   | 
25 | func a(x: Sub): Int32 {
   |      ^ 
   | 
note: found candidate
  ==> func_assign_var.cj:29:6:
   | 
29 | func a(x: Int32, y: Int32): Int32 {
   |      ^ 
   | 

3 errors generated, 3 errors printed.
*/
