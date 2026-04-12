// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE)EXEC: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// (not CJNATIVE) EXEC: %frontendCompiler %f

interface I1 {
    func foo(): Unit
}

interface I2 {}

open class A<T> {}

extend<T> A<T> where T <: I2 {
    public func foo(): Unit {
        println(2)
    }
}

class B<T> <: A<T> & I1 {} // error

main() {}

/* SCAN
error: interface function 'foo' must be implemented in 'B'
  ==> implement_by_super_extend01.cj:24:1:
   | 
24 | class B<T> <: A<T> & I1 {} // error
   | ^ 
   | 
note: member function 'foo' in 'A''s extension may not be visible to 'B'
  ==> implement_by_super_extend01.cj:19:17:
   | 
19 |     public func foo(): Unit {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
