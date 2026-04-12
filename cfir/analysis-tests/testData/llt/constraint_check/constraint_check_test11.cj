// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: constraint sanity check when upperbound has other generic parameter

interface I {}

struct R <: I {}

func foo<T, U>() where T <: U & R, U <: Int32 {}

main(): Int64 {
    return 1
}

/* SCAN
error: the upper bound 'Generics-U' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test11.cj:14:10:
   | 
14 | func foo<T, U>() where T <: U & R, U <: Int32 {}
   |          ^ 
   | 

error: the upper bound 'Int32' of generic parameter 'Generics-U' must be class or interface
  ==> constraint_check_test11.cj:14:13:
   | 
14 | func foo<T, U>() where T <: U & R, U <: Int32 {}
   |             ^ 
   | 

2 errors generated, 2 errors printed.
*/
