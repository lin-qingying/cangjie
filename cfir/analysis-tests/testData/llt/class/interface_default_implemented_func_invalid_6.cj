// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

interface I<T> {
    func foo(x: T) {
        return x
    }
}

open class A<X, Y> <: I<X> & I<Y> {}

class B<X, Y> <: A<X, Y> {
    init(a: X, b: Y) {}
}

func test<X>(a: X) {
    let x = B<X, X>(a, a)
    let y = B(a, a)
}

main(): Int64 {
    let x: Int32 = 1
    var a = test(x)
    var b = test<Int64>(1)
    return 0
}

/* SCAN
error: generic instantiation 'B<Generics-X, Generics-X>' causes ambiguous function 'foo'
  ==> interface_default_implemented_func_invalid_6.cj:22:13:
   | 
22 |     let x = B<X, X>(a, a)
   |             ^^^^^^^ 
   | 
note: found candidate
  ==> interface_default_implemented_func_invalid_6.cj:10:10:
   | 
10 |     func foo(x: T) {
   |          ^^^ 
   | 

error: generic instantiation 'B<Generics-X, Generics-X>' causes ambiguous function 'foo'
  ==> interface_default_implemented_func_invalid_6.cj:23:13:
   | 
23 |     let y = B(a, a)
   |             ^ 
   | 
note: found candidate
  ==> interface_default_implemented_func_invalid_6.cj:10:10:
   | 
10 |     func foo(x: T) {
   |          ^^^ 
   | 

error: generic instantiation 'test<Int32>' causes ambiguous function 'foo'
  ==> interface_default_implemented_func_invalid_6.cj:28:13:
   | 
28 |     var a = test(x)
   |             ^^^^ 
   | 
note: found candidate
  ==> interface_default_implemented_func_invalid_6.cj:10:10:
   | 
10 |     func foo(x: T) {
   |          ^^^ 
   | 

error: generic instantiation 'test<Int64>' causes ambiguous function 'foo'
  ==> interface_default_implemented_func_invalid_6.cj:29:13:
   | 
29 |     var b = test<Int64>(1)
   |             ^^^^^^^^^^^ 
   | 
note: found candidate
  ==> interface_default_implemented_func_invalid_6.cj:10:10:
   | 
10 |     func foo(x: T) {
   |          ^^^ 
   | 

4 errors generated, 4 errors printed.
*/
