// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I<T> {}

class A<M, N> {}

extend A<String, String> <: I<String> {}

extend<T, D> A<T, D> <: I<D> {}

main() {}

/* SCAN
error: interface 'Interface-I<Struct-String>' has been implemented by 'Class-A<Generics-M, Generics-N>', please remove it
  ==> duplicate_impl_3.cj:13:29:
   | 
13 | extend A<String, String> <: I<String> {}
   |                             ^^^^^^^^^ 
   | 
note: it may be an instantiated version of the following declaration
  ==> duplicate_impl_3.cj:15:1:
   | 
15 | extend<T, D> A<T, D> <: I<D> {}
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
