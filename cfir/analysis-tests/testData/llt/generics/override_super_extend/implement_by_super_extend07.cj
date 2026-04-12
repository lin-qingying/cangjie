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

interface I3 <: I2 {}

open class A<T> {}

extend<T> A<T> where T <: I2 {
    public func foo(): Unit {
        println(2)
    }
}

class B<T1> <: A<T1> {}

extend<T2> B<T2> <: I1 {} // error

class C<T1> <: A<T1> {}

extend<T2> C<T2> <: I1 where T2 <: I2 {} // ok

class D<T1> <: A<T1> where T1 <: I2 {}

extend<T2> D<T2> <: I1 {} // ok

main() {
    0
}

/* SCAN
error: interface function 'foo' must be implemented in 'extend B'
  ==> implement_by_super_extend07.cj:28:1:
   | 
28 | extend<T2> B<T2> <: I1 {} // error
   | ^ 
   | 
note: member function 'foo' in 'A''s extension may not be visible to 'extend B'
  ==> implement_by_super_extend07.cj:21:17:
   | 
21 |     public func foo(): Unit {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
