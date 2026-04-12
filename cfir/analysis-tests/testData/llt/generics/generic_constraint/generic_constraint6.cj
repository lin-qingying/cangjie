// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class Base {}

class Sub <: Base {}

class A<X> {
    public func f<Y>(x: Y): X where Y <: X {
        x
    }
}

class B<X> where X <: Base {
    public func f<Y>(x: Y): X where Y <: X {
        x
    }
}

main() {
    A<Int64>().f<Int64>(1)
    B<Base>().f<Sub>(Sub())
    0
}

/* SCAN
error: the upper bound 'Generics-X' of generic parameter 'Generics-Y' must be class or interface
  ==> generic_constraint6.cj:14:19:
   | 
14 |     public func f<Y>(x: Y): X where Y <: X {
   |                   ^ 
   | 

error: the upper bound 'Generics-X' of generic parameter 'Generics-Y' must be class or interface
  ==> generic_constraint6.cj:20:19:
   | 
20 |     public func f<Y>(x: Y): X where Y <: X {
   |                   ^ 
   | 

2 errors generated, 2 errors printed.
*/
