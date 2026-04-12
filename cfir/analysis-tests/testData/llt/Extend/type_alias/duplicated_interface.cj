// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {}

class Foo <: I1 {}

extend Foo <: I1 {}

interface I2<T> {}

type i2<T> = I2<T>

class Goo<T> <: I2<Int32> {}

extend<T> Goo<T> <: i2<Int32> {}

/* SCAN
error: interface 'Interface-I1' has been implemented by 'Class-Foo', please remove it
  ==> duplicated_interface.cj:13:15:
   | 
13 | extend Foo <: I1 {}
   |               ^^ 
   | 

error: interface 'Interface-I2<Int32>' has been implemented by 'Class-Goo<Generics-T>', please remove it
  ==> duplicated_interface.cj:21:21:
   | 
21 | extend<T> Goo<T> <: i2<Int32> {}
   |                     ^^^^^^^^^ 
   | 

error: 'main' is missing
 ==> duplicated_interface.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
