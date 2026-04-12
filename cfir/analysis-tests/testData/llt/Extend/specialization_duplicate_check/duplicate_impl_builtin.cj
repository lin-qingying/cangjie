// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I<T> {}

extend CPointer<Int64> <: I<Int64> {} // error

extend<T> CPointer<T> <: I<T> {} // error

main() {}

/* SCAN
error: interface 'Interface-I<Int64>' has been implemented by 'CPointer<Generics-T>', please remove it
  ==> duplicate_impl_builtin.cj:11:27:
   | 
11 | extend CPointer<Int64> <: I<Int64> {} // error
   |                           ^^^^^^^^ 
   | 
note: it may be an instantiated version of the following declaration
  ==> duplicate_impl_builtin.cj:13:1:
   | 
13 | extend<T> CPointer<T> <: I<T> {} // error
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
