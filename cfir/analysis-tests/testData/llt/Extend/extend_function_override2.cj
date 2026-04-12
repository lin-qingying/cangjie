// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

main(): Int64 {
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
    public operator func ==(right: B<T>): Bool {
        return true
    }
}

/* SCAN
error: cannot override function 'foo' in extend of supertype
  ==> extend_function_override2.cj:22:17:
   | 
22 |     public func foo() {
   |                 ^^^ 
   | 
note: overridden definition of 'foo' is here
  ==> extend_function_override2.cj:16:17:
   | 
16 |     public func foo() {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
