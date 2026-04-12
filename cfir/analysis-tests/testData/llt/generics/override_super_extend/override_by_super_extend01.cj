// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE)EXEC: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// (not CJNATIVE) EXEC: %frontendCompiler %f

interface I1 {
    func foo(): Unit {
        println(1)
    }
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
error: cannot override function 'foo'
  ==> override_by_super_extend01.cj:26:1:
   | 
26 | class B<T> <: A<T> & I1 {} // error
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
note: member function 'foo' in 'A''s extension conflict with that in interface 'I1'
  ==> override_by_super_extend01.cj:21:17:
   | 
21 |     public func foo(): Unit {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
