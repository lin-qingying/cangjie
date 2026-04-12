// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: constraint sanity check when upperbound has other generic parameter

struct R {}

interface I1 {}

interface I2 {}

extend Int32 <: I1 {}

func bar<T, U, Z>() where T <: U & R, U <: Z, Z <: Int32 {}

func noo<T, U, Z>() where T <: U & I1 & I2, U <: Z, Z <: Int32 {}

main(): Int64 {
    return 1
}

/* SCAN
error: the upper bound 'Generics-U' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test11_5.cj:18:10:
   | 
18 | func bar<T, U, Z>() where T <: U & R, U <: Z, Z <: Int32 {}
   |          ^ 
   | 

error: the upper bound 'Generics-Z' of generic parameter 'Generics-U' must be class or interface
  ==> constraint_check_test11_5.cj:18:13:
   | 
18 | func bar<T, U, Z>() where T <: U & R, U <: Z, Z <: Int32 {}
   |             ^ 
   | 

error: the upper bound 'Int32' of generic parameter 'Generics-Z' must be class or interface
  ==> constraint_check_test11_5.cj:18:16:
   | 
18 | func bar<T, U, Z>() where T <: U & R, U <: Z, Z <: Int32 {}
   |                ^ 
   | 

error: the upper bound 'Generics-U' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test11_5.cj:20:10:
   | 
20 | func noo<T, U, Z>() where T <: U & I1 & I2, U <: Z, Z <: Int32 {}
   |          ^ 
   | 

error: the upper bound 'Generics-Z' of generic parameter 'Generics-U' must be class or interface
  ==> constraint_check_test11_5.cj:20:13:
   | 
20 | func noo<T, U, Z>() where T <: U & I1 & I2, U <: Z, Z <: Int32 {}
   |             ^ 
   | 

error: the upper bound 'Int32' of generic parameter 'Generics-Z' must be class or interface
  ==> constraint_check_test11_5.cj:20:16:
   | 
20 | func noo<T, U, Z>() where T <: U & I1 & I2, U <: Z, Z <: Int32 {}
   |                ^ 
   | 

6 errors generated, 6 errors printed.
*/
