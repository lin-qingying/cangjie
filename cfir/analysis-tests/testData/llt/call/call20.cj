// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix

open class Base {}

class Child <: Base {}

class A {
    public static func a<T, K>(x: T, b: K): Bool where T <: K {
        return true
    }
}

main(): Int64 {
    var obj = A()
    if (A.a<Child, Base>(Child(), Base()) != true) {
        return 1
    }
    return 0
}

/* SCAN
error: the upper bound 'Generics-K' of generic parameter 'Generics-T' must be class or interface
  ==> call20.cj:14:26:
   | 
14 |     public static func a<T, K>(x: T, b: K): Bool where T <: K {
   |                          ^ 
   | 

1 error generated, 1 error printed.
*/
