// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I {
    func f(): Unit
}

open class C1 {}

class C2 <: C1 {}

class A<T> {}

extend<T> A<T> where T <: C2 {
    public func f() {}
}

extend<T> A<T> <: I where T <: C1 {}

/* SCAN
error: implementation of function or property is needed in 'extend A'
  ==> implement_in_strict.cj:23:1:
   | 
23 | extend<T> A<T> <: I where T <: C1 {}
   | ^ 
   | 
note: unimplemented interface function 'f'
  ==> implement_in_strict.cj:10:5:
   | 
10 |     func f(): Unit
   |     ^ 
   | 

error: 'main' is missing
 ==> implement_in_strict.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
