// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: constraint sanity check when upperbound has other generic parameter

interface I {}

class D<T> {}

func bar<T, U>() where T <: D<U> & D<I>, U <: I {}

main(): Int64 {
    return 1
}

/* SCAN
error: generic parameter 'T' cannot have two or more class upper bounds 'Class-D<Generics-U>, Class-D<Interface-I>' without subtype relation
  ==> constraint_check_test11_3.cj:14:10:
   | 
14 | func bar<T, U>() where T <: D<U> & D<I>, U <: I {}
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
