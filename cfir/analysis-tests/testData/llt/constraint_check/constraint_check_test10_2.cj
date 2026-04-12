// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: rule 4: The class-irrelevant types cannot exist with classlike type in same time

interface I {}

interface I2 {}

struct R <: I {}

extend Int32 <: I {}

func goo<T>() where T <: Int32 & I {} // OK, Int32 is the subtype of Interface I.

func noo<T>() where T <: R & I {} // OK, struct R is the subtype of interface I.

func foo<T>() where T <: I & I2 {} // OK

main(): Int64 {
    return 0
}

/* SCAN
error: the upper bound 'Int32' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test10_2.cj:18:10:
   | 
18 | func goo<T>() where T <: Int32 & I {} // OK, Int32 is the subtype of Interface I.
   |          ^ 
   | 

error: the upper bound 'Struct-R' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test10_2.cj:20:10:
   | 
20 | func noo<T>() where T <: R & I {} // OK, struct R is the subtype of interface I.
   |          ^ 
   | 

2 errors generated, 2 errors printed.
*/
