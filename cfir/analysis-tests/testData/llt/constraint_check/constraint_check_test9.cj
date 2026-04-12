// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: rule 2: There can not be two or more class irrelevant upper bounds.

open class Base {}

class Balala {}

func foo1<T>() where T <: Int32 & Bool {}

func bar1<T>() where T <: () -> Int32 & () -> Bool {}

func goo1<T>() where T <: (Int32, Bool) & (Int64, Bool) {}

func bar2<T>() where T <: () -> Base & () -> Balala {}

func goo2<T>() where T <: (Base, Bool) & (Balala, Bool) {}

main(): Int64 {
    return 1
}

/* SCAN
error: the upper bound 'Bool' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test9.cj:14:11:
   | 
14 | func foo1<T>() where T <: Int32 & Bool {}
   |           ^ 
   | 

error: the upper bound '() -> Bool' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test9.cj:16:11:
   | 
16 | func bar1<T>() where T <: () -> Int32 & () -> Bool {}
   |           ^ 
   | 

error: the upper bound 'Tuple<Int32, Bool>' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test9.cj:18:11:
   | 
18 | func goo1<T>() where T <: (Int32, Bool) & (Int64, Bool) {}
   |           ^ 
   | 

error: the upper bound '() -> Class-Balala' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test9.cj:20:11:
   | 
20 | func bar2<T>() where T <: () -> Base & () -> Balala {}
   |           ^ 
   | 

error: the upper bound 'Tuple<Class-Balala, Bool>' of generic parameter 'Generics-T' must be class or interface
  ==> constraint_check_test9.cj:22:11:
   | 
22 | func goo2<T>() where T <: (Base, Bool) & (Balala, Bool) {}
   |           ^ 
   | 

5 errors generated, 5 errors printed.
*/
