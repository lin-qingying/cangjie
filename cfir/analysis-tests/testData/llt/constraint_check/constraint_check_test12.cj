// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// UpperBound: rule 1: There can be no recursive constraint if the upper bound of a type argument is a class irrelevant type.

enum Option<T> {
    None
    | Some(T)
}

struct L<T> {}

func foo0<T>(a: T) where T <: Array<T> {}

func foo1<T>(a: T) where T <: (T, Int32) {}

func foo2<T>(a: T) where T <: (T) -> Int32 {}

func foo3<T>(a: T) where T <: (Int32) -> T {}

func foo4<T>(a: T) where T <: L<T> {}

func foo5<T>(a: T) where T <: Option<T> {}

func foo6<T, U>(a: T, b: U) where T <: Option<U>, U <: Option<T> {}

func foo7<T, U>(a: T, b: U) where T <: (U) -> Int32, U <: (T) -> Int32 {}

main(): Int64 {
    return 1
}

/* SCAN
error: generic parameter 'T' cannot be used in class irrelevant upper bounds 'Struct-Array<Generics-T>'
  ==> constraint_check_test12.cj:17:11:
   | 
17 | func foo0<T>(a: T) where T <: Array<T> {}
   |           ^ 
   | 

error: generic parameter 'T' cannot be used in class irrelevant upper bounds 'Tuple<Generics-T, Int32>'
  ==> constraint_check_test12.cj:19:11:
   | 
19 | func foo1<T>(a: T) where T <: (T, Int32) {}
   |           ^ 
   | 

error: generic parameter 'T' cannot be used in class irrelevant upper bounds '(Generics-T) -> Int32'
  ==> constraint_check_test12.cj:21:11:
   | 
21 | func foo2<T>(a: T) where T <: (T) -> Int32 {}
   |           ^ 
   | 

error: generic parameter 'T' cannot be used in class irrelevant upper bounds '(Int32) -> Generics-T'
  ==> constraint_check_test12.cj:23:11:
   | 
23 | func foo3<T>(a: T) where T <: (Int32) -> T {}
   |           ^ 
   | 

error: generic parameter 'T' cannot be used in class irrelevant upper bounds 'Struct-L<Generics-T>'
  ==> constraint_check_test12.cj:25:11:
   | 
25 | func foo4<T>(a: T) where T <: L<T> {}
   |           ^ 
   | 

error: generic parameter 'T' cannot be used in class irrelevant upper bounds 'Enum-Option<Generics-T>'
  ==> constraint_check_test12.cj:27:11:
   | 
27 | func foo5<T>(a: T) where T <: Option<T> {}
   |           ^ 
   | 

error: generic parameter 'T' cannot be used in class irrelevant upper bounds 'Enum-Option<Generics-U>'
  ==> constraint_check_test12.cj:29:11:
   | 
29 | func foo6<T, U>(a: T, b: U) where T <: Option<U>, U <: Option<T> {}
   |           ^ 
   | 

error: generic parameter 'T' cannot be used in class irrelevant upper bounds '(Generics-U) -> Int32'
  ==> constraint_check_test12.cj:31:11:
   | 
31 | func foo7<T, U>(a: T, b: U) where T <: (U) -> Int32, U <: (T) -> Int32 {}
   |           ^ 
   | 

8 errors generated, 8 errors printed.
*/
