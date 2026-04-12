// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: constraint sanity check when upperbound has other generic parameter

struct R {}

open class Base {}

class Child <: Base {}

interface I1 {}

interface I2 {}

extend Int32 <: I1 {}

func doo<T, U, Z>() where T <: U, U <: Z, Z <: T {}

func foo<T, U, Z>() where T <: U & R, U <: Z, Z <: R {}

func bar<T, U, Z>() where T <: U & Child, U <: Z, Z <: Base {}

func goo<T, U, Z>() where T <: U & I1 & I2, U <: Z, Z <: Base {}

func noo<T, U, Z>() where T <: U & I1, U <: Z, Z <: Int32 {}

main(): Int64 {
    return 0
}

/* SCAN
error: generic parameter 'T' is bounded directly recursively with 'Generics-U' which is forbidden
  ==> constraint_check_test11_4.cj:22:10:
   | 
22 | func doo<T, U, Z>() where T <: U, U <: Z, Z <: T {}
   |          ^ 
   | 

error: the upper bound 'Generics-U' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test11_4.cj:24:10:
   | 
24 | func foo<T, U, Z>() where T <: U & R, U <: Z, Z <: R {}
   |          ^ 
   | 

error: the upper bound 'Generics-Z' of generic parameter 'Generics-U' must be class or interface
  ==> constraint_check_test11_4.cj:24:13:
   | 
24 | func foo<T, U, Z>() where T <: U & R, U <: Z, Z <: R {}
   |             ^ 
   | 

error: the upper bound 'Struct-R' of generic parameter 'Generics-Z' must be class or interface
  ==> constraint_check_test11_4.cj:24:16:
   | 
24 | func foo<T, U, Z>() where T <: U & R, U <: Z, Z <: R {}
   |                ^ 
   | 

error: the upper bound 'Generics-U' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test11_4.cj:26:10:
   | 
26 | func bar<T, U, Z>() where T <: U & Child, U <: Z, Z <: Base {}
   |          ^ 
   | 

error: the upper bound 'Generics-Z' of generic parameter 'Generics-U' must be class or interface
  ==> constraint_check_test11_4.cj:26:13:
   | 
26 | func bar<T, U, Z>() where T <: U & Child, U <: Z, Z <: Base {}
   |             ^ 
   | 

error: the upper bound 'Generics-U' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test11_4.cj:28:10:
   | 
28 | func goo<T, U, Z>() where T <: U & I1 & I2, U <: Z, Z <: Base {}
   |          ^ 
   | 

error: the upper bound 'Generics-Z' of generic parameter 'Generics-U' must be class or interface
  ==> constraint_check_test11_4.cj:28:13:
   | 
28 | func goo<T, U, Z>() where T <: U & I1 & I2, U <: Z, Z <: Base {}
   |             ^ 
   | 

error: the upper bound 'Generics-U' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test11_4.cj:30:10:
   | 
30 | func noo<T, U, Z>() where T <: U & I1, U <: Z, Z <: Int32 {}
   |          ^ 
   | 

error: the upper bound 'Generics-Z' of generic parameter 'Generics-U' must be class or interface
  ==> constraint_check_test11_4.cj:30:13:
   | 
30 | func noo<T, U, Z>() where T <: U & I1, U <: Z, Z <: Int32 {}
   |             ^ 
   | 

error: the upper bound 'Int32' of generic parameter 'Generics-Z' must be class or interface
  ==> constraint_check_test11_4.cj:30:16:
   | 
30 | func noo<T, U, Z>() where T <: U & I1, U <: Z, Z <: Int32 {}
   |                ^ 
   | 

11 errors generated, 11 errors printed.
*/
