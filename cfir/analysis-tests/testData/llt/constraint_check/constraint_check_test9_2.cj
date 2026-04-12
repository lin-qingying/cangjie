// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: rule 2: There can not be two or more class-irrelevant upper bounds.

open class Base {}

class Child <: Base {}

func bar2<T>() where T <: () -> Base & () -> Child {}

func goo2<T>() where T <: (Base, Base) & (Child, Child) {}

main(): Int64 {
    return 0
}

/* SCAN
error: the upper bound '() -> Class-Base' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test9_2.cj:14:11:
   | 
14 | func bar2<T>() where T <: () -> Base & () -> Child {}
   |           ^ 
   | 

error: the upper bound 'Tuple<Class-Base, Class-Base>' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test9_2.cj:16:11:
   | 
16 | func goo2<T>() where T <: (Base, Base) & (Child, Child) {}
   |           ^ 
   | 

2 errors generated, 2 errors printed.
*/
