// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I0 {}

interface I1 <: I0 {}

interface I2 <: I0 {}

open class C1 <: I1 & I2 {
    func h(a: I1): Unit {}
}

class C2 <: C1 & I2 {
    func h(a: I2): Unit {}
}

func f<T>(a: T): Unit where T <: C1 & C2 {
    var lam1 = a.h
    var lam2: (I0) -> Unit = a.h
}

main() {}

/* SCAN
error: ambiguous use of 'h'
  ==> expose6.cj:24:16:
   | 
24 |     var lam1 = a.h
   |                ^^^ 
   | 
note: found candidate
  ==> expose6.cj:16:10:
   | 
16 |     func h(a: I1): Unit {}
   |          ^ 
   | 
note: found candidate
  ==> expose6.cj:20:10:
   | 
20 |     func h(a: I2): Unit {}
   |          ^ 
   | 

error: no matching function declaration for function reference 'h'
  ==> expose6.cj:25:30:
   | 
25 |     var lam2: (I0) -> Unit = a.h
   |                              ^ 
   | 

2 errors generated, 2 errors printed.
*/
