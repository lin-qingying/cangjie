// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A<K, U> where U <: K, K <: U {
    func foo(x: Box<U>): Box<K> {
        x as Box<K> ?? throw Exception()
    }
}

func foo<K, U>(x: Box<U>): Box<K> where U <: K, K <: U {
    x as Box<K> ?? throw Exception()
}

class B<K, U, X, Y, Z> where K <: U, U <: X, X <: Y, Y <: Z, Z <: U {
    func foo(x: Box<Z>): Box<K> {
        x as Box<K> ?? throw Exception()
    }
}

main(): Int64 {
    0
}

/* SCAN
error: generic parameter 'K' is bounded directly recursively with 'Generics-U' which is forbidden
 ==> recursive_generics.cj:9:9:
  | 
9 | class A<K, U> where U <: K, K <: U {
  |         ^ 
  | 

error: generic parameter 'K' is bounded directly recursively with 'Generics-U' which is forbidden
  ==> recursive_generics.cj:15:10:
   | 
15 | func foo<K, U>(x: Box<U>): Box<K> where U <: K, K <: U {
   |          ^ 
   | 

error: generic parameter 'K' is bounded directly recursively with 'Generics-U' which is forbidden
  ==> recursive_generics.cj:19:9:
   | 
19 | class B<K, U, X, Y, Z> where K <: U, U <: X, X <: Y, Y <: Z, Z <: U {
   |         ^ 
   | 

3 errors generated, 3 errors printed.
*/
