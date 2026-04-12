// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I<T> {}

class A<K> {}

extend A<String> <: I<String> {} // error

extend<T> A<T> <: I<T> {} // error

main() {}

/* SCAN
error: interface 'Interface-I<Struct-String>' has been implemented by 'Class-A<Generics-K>', please remove it
  ==> duplicate_impl_1.cj:13:21:
   | 
13 | extend A<String> <: I<String> {} // error
   |                     ^^^^^^^^^ 
   | 
note: it may be an instantiated version of the following declaration
  ==> duplicate_impl_1.cj:15:1:
   | 
15 | extend<T> A<T> <: I<T> {} // error
   | ^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
