// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: rule 4: The class-irrelevant types cannot exist with classlike type in same time

class C {}

interface I {}

struct R <: I {}

func bar<T>() where T <: Bool & C {}

func goo<T>() where T <: Bool & R & C {}

func noo<T>() where T <: () -> R & C {}

func doo<T>() where T <: (R, R) & C {}

main(): Int64 {
    return 1
}

/* SCAN
error: the upper bound 'Bool' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test10.cj:16:10:
   | 
16 | func bar<T>() where T <: Bool & C {}
   |          ^ 
   | 

error: the upper bound 'Bool' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test10.cj:18:10:
   | 
18 | func goo<T>() where T <: Bool & R & C {}
   |          ^ 
   | 

error: the upper bound '() -> Struct-R' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test10.cj:20:10:
   | 
20 | func noo<T>() where T <: () -> R & C {}
   |          ^ 
   | 

error: the upper bound 'Tuple<Struct-R, Struct-R>' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test10.cj:22:10:
   | 
22 | func doo<T>() where T <: (R, R) & C {}
   |          ^ 
   | 

4 errors generated, 4 errors printed.
*/
