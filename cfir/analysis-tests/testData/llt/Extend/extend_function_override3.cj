// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

main(): Int64 {
    var a = B<Int32>().foo()
    return 0
}

open class A<T> {}

extend<T> A<T> {
    public func foo() {
        0
    }
}

class B<T> <: A<T> {
    public func foo() {
        1
    }
}

/* SCAN
error: ambiguous match for function call 'foo'
  ==> extend_function_override3.cj:10:24:
   | 
10 |     var a = B<Int32>().foo()
   |                        ^ 
   | 
note: found candidate
  ==> extend_function_override3.cj:17:17:
   | 
17 |     public func foo() {
   |                 ^ 
   | 
note: found candidate
  ==> extend_function_override3.cj:23:17:
   | 
23 |     public func foo() {
   |                 ^ 
   | 

error: cannot override function 'foo' in extend of supertype
  ==> extend_function_override3.cj:23:17:
   | 
23 |     public func foo() {
   |                 ^^^ 
   | 
note: overridden definition of 'foo' is here
  ==> extend_function_override3.cj:17:17:
   | 
17 |     public func foo() {
   |                 ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
