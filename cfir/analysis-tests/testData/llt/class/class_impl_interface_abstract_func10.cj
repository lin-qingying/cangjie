// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface I<T> {
    func foo(a: T): Int64
}

class C<T, V> <: I<T> & I<V> {}

main() {
    let c = C<Int64, String>()
    return 0
}

/* SCAN
error: class 'C' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> class_impl_interface_abstract_func10.cj:13:1:
   | 
13 | class C<T, V> <: I<T> & I<V> {}
   | ^ 
   | 
note: unimplemented interface function 'foo', of type: (Generics-T) -> Int64
  ==> class_impl_interface_abstract_func10.cj:10:5:
   | 
10 |     func foo(a: T): Int64
   |     ^ 
   | 
note: unimplemented interface function 'foo', of type: (Generics-V) -> Int64
  ==> class_impl_interface_abstract_func10.cj:10:5:
   | 
10 |     func foo(a: T): Int64
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
