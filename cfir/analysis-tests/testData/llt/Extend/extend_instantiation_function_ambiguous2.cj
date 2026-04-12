// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

main(): Int64 {
    var a = A<Int32>()
    var b = B<Int32>()
    return 0
}

class A<T> {
    public func foo(x: T) {
        return 1
    }
}

extend<T> A<T> {
    public func foo(x: Int32) {
        return 2
    }
}

struct B<T> {
    public func foo(x: T) {
        return 1
    }
}

extend<T> B<T> {
    public func foo(x: Int32) {
        return 2
    }
}

/* SCAN
error: generic instantiation 'A<Int32>' causes ambiguous function 'foo'
  ==> extend_instantiation_function_ambiguous2.cj:10:13:
   | 
10 |     var a = A<Int32>()
   |             ^^^^^^^^ 
   | 
note: found candidate
  ==> extend_instantiation_function_ambiguous2.cj:16:17:
   | 
16 |     public func foo(x: T) {
   |                 ^^^ 
   | 
note: found candidate
  ==> extend_instantiation_function_ambiguous2.cj:22:17:
   | 
22 |     public func foo(x: Int32) {
   |                 ^^^ 
   | 

error: generic instantiation 'B<Int32>' causes ambiguous function 'foo'
  ==> extend_instantiation_function_ambiguous2.cj:11:13:
   | 
11 |     var b = B<Int32>()
   |             ^^^^^^^^ 
   | 
note: found candidate
  ==> extend_instantiation_function_ambiguous2.cj:28:17:
   | 
28 |     public func foo(x: T) {
   |                 ^^^ 
   | 
note: found candidate
  ==> extend_instantiation_function_ambiguous2.cj:34:17:
   | 
34 |     public func foo(x: Int32) {
   |                 ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
