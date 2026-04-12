// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1<T> {}

interface I2<T> <: I1<T> {}

class A<T> <: I2<T> {}

extend A<String> <: I1<String> {} // error

main() {}

/* SCAN
error: interface 'Interface-I1<Struct-String>' has been implemented by 'Class-A<Generics-T>', please remove it
  ==> duplicate_impl_5_2.cj:15:21:
   | 
15 | extend A<String> <: I1<String> {} // error
   |                     ^^^^^^^^^^ 
   | 
note: it may be an instantiated version of the following declaration
  ==> duplicate_impl_5_2.cj:13:1:
   | 
13 | class A<T> <: I2<T> {}
   | ^^^^^^^^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
