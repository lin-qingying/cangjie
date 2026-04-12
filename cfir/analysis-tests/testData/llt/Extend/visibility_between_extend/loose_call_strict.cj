// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {}

interface I2 <: I1 {}

open class C1 <: I2 {
    static var i = 1
}

class C2 <: C1 {}

class A<T, R> {}

extend<T, R> A<T, R> where T <: C1, R <: I2 {
    func f() {
        return T.i
    }
}

extend<R, T> A<R, T> where T <: I1, R <: C2 {
    func f1() {
        return f()
    }
}

extend<R, T> A<R, T> where T <: I2, R <: I2 {
    func f2() {
        return f()
    }
}

extend<R, T> A<R, T> where T <: I1, R <: I2 {
    func f3() {
        return f()
    }
}

/* SCAN
error: undeclared identifier 'f'
  ==> loose_call_strict.cj:29:16:
   | 
29 |         return f()
   |                ^ 
   | 

error: undeclared identifier 'f'
  ==> loose_call_strict.cj:35:16:
   | 
35 |         return f()
   |                ^ 
   | 

error: undeclared identifier 'f'
  ==> loose_call_strict.cj:41:16:
   | 
41 |         return f()
   |                ^ 
   | 

error: 'main' is missing
 ==> loose_call_strict.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

4 errors generated, 4 errors printed.
*/
