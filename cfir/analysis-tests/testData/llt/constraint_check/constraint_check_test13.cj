// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: rule 4: The class-irrelevant types cannot exist with classlike type in same time 

class C {}

interface I {}

struct R {}

func foo<T>() where T <: R & I {}

main(): Int64 {
    return 1
}

/* SCAN
error: the upper bound 'Struct-R' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test13.cj:16:10:
   | 
16 | func foo<T>() where T <: R & I {}
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
