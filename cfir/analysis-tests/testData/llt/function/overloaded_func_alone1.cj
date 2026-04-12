// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var a = foo
    var b: Int64 = match (foo) {
        case x: () -> Unit => 1
        case x: (Float32) -> Int64 => 0
        case x: (Float64) -> Int64 => 3
        case _ => 4
    }
    if (b != 0) {
        return 1
    }
    foo
    var c = match {
        case true => foo
        case _ => foo
    }
    return 0
}

public func foo(x: Float32) {
    return Int64(x)
}

public func foo(x: Float64) {
    return Int64(x)
}

public func foo(x: Float64, y: String) {
    return Int64(x)
}

/* SCAN
error: ambiguous use of 'foo'
  ==> overloaded_func_alone1.cj:10:13:
   | 
10 |     var a = foo
   |             ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:28:13:
   | 
28 | public func foo(x: Float32) {
   |             ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:32:13:
   | 
32 | public func foo(x: Float64) {
   |             ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:36:13:
   | 
36 | public func foo(x: Float64, y: String) {
   |             ^^^ 
   | 

error: ambiguous use of 'foo'
  ==> overloaded_func_alone1.cj:11:27:
   | 
11 |     var b: Int64 = match (foo) {
   |                           ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:28:13:
   | 
28 | public func foo(x: Float32) {
   |             ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:32:13:
   | 
32 | public func foo(x: Float64) {
   |             ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:36:13:
   | 
36 | public func foo(x: Float64, y: String) {
   |             ^^^ 
   | 

error: ambiguous use of 'foo'
  ==> overloaded_func_alone1.cj:20:5:
   | 
20 |     foo
   |     ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:28:13:
   | 
28 | public func foo(x: Float32) {
   |             ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:32:13:
   | 
32 | public func foo(x: Float64) {
   |             ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:36:13:
   | 
36 | public func foo(x: Float64, y: String) {
   |             ^^^ 
   | 

error: ambiguous use of 'foo'
  ==> overloaded_func_alone1.cj:22:22:
   | 
22 |         case true => foo
   |                      ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:28:13:
   | 
28 | public func foo(x: Float32) {
   |             ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:32:13:
   | 
32 | public func foo(x: Float64) {
   |             ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:36:13:
   | 
36 | public func foo(x: Float64, y: String) {
   |             ^^^ 
   | 

error: ambiguous use of 'foo'
  ==> overloaded_func_alone1.cj:23:19:
   | 
23 |         case _ => foo
   |                   ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:28:13:
   | 
28 | public func foo(x: Float32) {
   |             ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:32:13:
   | 
32 | public func foo(x: Float64) {
   |             ^^^ 
   | 
note: found candidate
  ==> overloaded_func_alone1.cj:36:13:
   | 
36 | public func foo(x: Float64, y: String) {
   |             ^^^ 
   | 

5 errors generated, 5 errors printed.
*/
