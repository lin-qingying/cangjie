// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I {}

struct R <: I {}

func bar<T, U>() where T <: U & R, U <: R {}

main(): Int64 {
    return 0
}

/* SCAN
error: the upper bound 'Generics-U' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test11_2.cj:13:10:
   | 
13 | func bar<T, U>() where T <: U & R, U <: R {}
   |          ^ 
   | 

error: the upper bound 'Struct-R' of generic parameter 'Generics-U' must be class or interface
  ==> constraint_check_test11_2.cj:13:13:
   | 
13 | func bar<T, U>() where T <: U & R, U <: R {}
   |             ^ 
   | 

2 errors generated, 2 errors printed.
*/
