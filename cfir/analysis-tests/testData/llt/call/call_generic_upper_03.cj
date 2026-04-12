// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I<T> {
    func foo(a: T) {}
}

interface I2 {}

open class A {}

open class B <: A {}

class C <: B & I2 {}

class D <: I<B> & I<I2> {} // exist func foo(a: B);  func foo(a: I2)

main() {
    let d = D()
    d.foo(C())
    return 1
}

/* SCAN
error: ambiguous method 'foo' match for generic member access function call when searching in upper bounds
  ==> call_generic_upper_03.cj:25:5:
   | 
25 |     d.foo(C())
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
