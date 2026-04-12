// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

struct A15 {
    public func foo(a: Float64) {
        return a
    }
}

extend A15 {
    public func foo(a: Int64) {
        return a
    }
}

extend A15 {
    public func foo(a: Int32) {
        return a
    }
}

extend A15 {
    public func foo(a: Rune) {
        return a
    }
}

extend A15 {
    public func foo(a: Float32) {
        return a
    }
}

extend A15 {
    public func foo(a: Int64) {
        return a
    }
}

let i = A15()
let i2 = i.foo ~> i.foo

/* SCAN
error: ambiguous use of 'foo'
  ==> extend_function_conflict.cj:46:10:
   | 
46 | let i2 = i.foo ~> i.foo
   |          ^^^^^ 
   | 
note: found candidate
  ==> extend_function_conflict.cj:10:17:
   | 
10 |     public func foo(a: Float64) {
   |                 ^^^ 
   | 
note: found candidate
  ==> extend_function_conflict.cj:16:17:
   | 
16 |     public func foo(a: Int64) {
   |                 ^^^ 
   | 
note: found candidate
  ==> extend_function_conflict.cj:22:17:
   | 
22 |     public func foo(a: Int32) {
   |                 ^^^ 
   | 
note: found candidate
  ==> extend_function_conflict.cj:28:17:
   | 
28 |     public func foo(a: Rune) {
   |                 ^^^ 
   | 
note: found candidate
  ==> extend_function_conflict.cj:34:17:
   | 
34 |     public func foo(a: Float32) {
   |                 ^^^ 
   | 
note: found candidate
  ==> extend_function_conflict.cj:40:17:
   | 
40 |     public func foo(a: Int64) {
   |                 ^^^ 
   | 

error: extend member 'foo' is not allowed to shadow members of 'extend A15'
  ==> extend_function_conflict.cj:16:17:
   | 
16 |     public func foo(a: Int64) {
   |                 ^^^ 
   | 

error: extend member 'foo' is not allowed to shadow members of 'extend A15'
  ==> extend_function_conflict.cj:40:17:
   | 
40 |     public func foo(a: Int64) {
   |                 ^^^ 
   | 

3 errors generated, 3 errors printed.
*/
