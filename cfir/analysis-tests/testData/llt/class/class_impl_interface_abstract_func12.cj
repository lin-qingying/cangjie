// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface I<T> {
    func foo1(a:T):Int64
    func foo2(a:T):Int64
    func foo3():Int64
}

class C<T, V> <: I<T> & I<V> {} 
class D<K> <: I<K> {}

main() {
    return 0
}

/* SCAN
error: class 'C' missing abstract modifier, otherwise abstract function or property should be implemented
 ==> class_impl_interface_abstract_func12.cj:9:1:
  | 
9 | class C<T, V> <: I<T> & I<V> {} 
  | ^ 
  | 
note: unimplemented interface function 'foo1', of type: (Generics-T) -> Int64
 ==> class_impl_interface_abstract_func12.cj:4:5:
  | 
4 |     func foo1(a:T):Int64
  |     ^ 
  | 
note: unimplemented interface function 'foo1', of type: (Generics-V) -> Int64
 ==> class_impl_interface_abstract_func12.cj:4:5:
  | 
4 |     func foo1(a:T):Int64
  |     ^ 
  | 
note: unimplemented interface function 'foo2', of type: (Generics-T) -> Int64
 ==> class_impl_interface_abstract_func12.cj:5:5:
  | 
5 |     func foo2(a:T):Int64
  |     ^ 
  | 
note: unimplemented interface function 'foo2', of type: (Generics-V) -> Int64
 ==> class_impl_interface_abstract_func12.cj:5:5:
  | 
5 |     func foo2(a:T):Int64
  |     ^ 
  | 
note: unimplemented interface function 'foo3'
 ==> class_impl_interface_abstract_func12.cj:6:5:
  | 
6 |     func foo3():Int64
  |     ^ 
  | 

error: class 'D' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> class_impl_interface_abstract_func12.cj:10:1:
   | 
10 | class D<K> <: I<K> {}
   | ^ 
   | 
note: unimplemented interface function 'foo1'
 ==> class_impl_interface_abstract_func12.cj:4:5:
  | 
4 |     func foo1(a:T):Int64
  |     ^ 
  | 
note: unimplemented interface function 'foo2'
 ==> class_impl_interface_abstract_func12.cj:5:5:
  | 
5 |     func foo2(a:T):Int64
  |     ^ 
  | 
note: unimplemented interface function 'foo3'
 ==> class_impl_interface_abstract_func12.cj:6:5:
  | 
6 |     func foo3():Int64
  |     ^ 
  | 

2 errors generated, 2 errors printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
