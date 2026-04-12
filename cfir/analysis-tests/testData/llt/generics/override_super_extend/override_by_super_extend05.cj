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

interface I2<T> {}

interface I3<T> <: I2<T> {}

open class A<T> {}

extend<T> A<T> where T <: I3<T> {
    public func foo(): Unit {
        println(2)
    }
}

class B<T> <: A<T> & I1 where T <: I2<T> {} // error

class C <: I3<C> {}

main() {
    B<C>().foo()
    0
}

/* SCAN
error: cannot override function 'foo'
  ==> override_by_super_extend05.cj:28:1:
   | 
28 | class B<T> <: A<T> & I1 where T <: I2<T> {} // error
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
note: member function 'foo' in 'A''s extension conflict with that in interface 'I1'
  ==> override_by_super_extend05.cj:23:17:
   | 
23 |     public func foo(): Unit {
   |                 ^^^ 
   | 

1 error generated, 1 error printed.
*/
