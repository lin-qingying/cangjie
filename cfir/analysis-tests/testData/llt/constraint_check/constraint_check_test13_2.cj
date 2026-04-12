// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: rule 4: The class-irrelevant types cannot exist with classlike type in same time

class C {}

interface I1 {}

interface I2 {}

struct R {}

extend R <: I1 & I2 {}

func foo<T>() where T <: R & I1 & I2 {}

main(): Int64 {
    return 0
}

/* SCAN
error: the upper bound 'Struct-R' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test13_2.cj:20:10:
   | 
20 | func foo<T>() where T <: R & I1 & I2 {}
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
