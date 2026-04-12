// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I<T> {
    func foo1(a:T):Int64
    func foo2(a:T):Int64
    func foo3():Int64
}

class C<T, V> {} 

class A{}
class B{}

extend C<A, B> <: I<A> & I<B> {}

main() {
    return 0
}

/* SCAN
error: implementation of function or property is needed in 'extend C'
  ==> extend_instantiated_type_14_err.cj:14:1:
   | 
14 | extend C<A, B> <: I<A> & I<B> {}
   | ^ 
   | 
note: unimplemented interface function 'foo1', of type: (Class-A) -> Int64
 ==> extend_instantiated_type_14_err.cj:4:5:
  | 
4 |     func foo1(a:T):Int64
  |     ^ 
  | 
note: unimplemented interface function 'foo1', of type: (Class-B) -> Int64
 ==> extend_instantiated_type_14_err.cj:4:5:
  | 
4 |     func foo1(a:T):Int64
  |     ^ 
  | 
note: unimplemented interface function 'foo2', of type: (Class-A) -> Int64
 ==> extend_instantiated_type_14_err.cj:5:5:
  | 
5 |     func foo2(a:T):Int64
  |     ^ 
  | 
note: unimplemented interface function 'foo2', of type: (Class-B) -> Int64
 ==> extend_instantiated_type_14_err.cj:5:5:
  | 
5 |     func foo2(a:T):Int64
  |     ^ 
  | 
note: unimplemented interface function 'foo3'
 ==> extend_instantiated_type_14_err.cj:6:5:
  | 
6 |     func foo3():Int64
  |     ^ 
  | 

1 error generated, 1 error printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
