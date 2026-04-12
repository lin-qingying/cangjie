// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: rule 3: If there are multiple classes then these classes, they must be in one inheritance chain.

open class Base {}

class Balala {}

func foo<T>() where T <: Base & Balala {}

main(): Int64 {
    return 1
}

/* SCAN
error: generic parameter 'T' cannot have two or more class upper bounds 'Class-Balala, Class-Base' without subtype relation
  ==> constraint_check_test8.cj:14:10:
   | 
14 | func foo<T>() where T <: Base & Balala {}
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
