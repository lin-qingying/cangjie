// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A<T> {}

interface Foo {
    func foo(): Unit {}
}

extend<T> A<T> <: Foo where T <: Int64 {}

func boo(x: Foo) {}

main(): Int64 {
    let a = A<Int32>()
    boo(a)
    a.foo()
    return 0
}

/* SCAN
error: the upper bound 'Int64' of generic parameter 'Generics-T' must be class or interface
  ==> extend_generic10.cj:15:8:
   | 
15 | extend<T> A<T> <: Foo where T <: Int64 {}
   |        ^ 
   | 

error: mismatched types
  ==> extend_generic10.cj:21:9:
   | 
21 |     boo(a)
   |         ^ expected 'Interface-Foo', found 'Class-A<Int32>'
   | 

error: 'foo' is not a member of class 'A<Int32>'
  ==> extend_generic10.cj:22:7:
   | 
22 |     a.foo()
   |       ^^^ 
   | 

3 errors generated, 3 errors printed.
*/
