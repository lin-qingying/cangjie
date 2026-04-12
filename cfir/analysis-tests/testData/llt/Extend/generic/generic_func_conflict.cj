// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

open class A {}

class B <: A {}

class C {}

extend C {
    static func f<A>(a: A) {}
}

extend C {
    static func f<X>(a: X) {}
}

/* SCAN
error: extend member 'f' is not allowed to shadow members of 'extend C'
  ==> generic_func_conflict.cj:16:17:
   | 
16 |     static func f<A>(a: A) {}
   |                 ^ 
   | 

error: extend member 'f' is not allowed to shadow members of 'extend C'
  ==> generic_func_conflict.cj:20:17:
   | 
20 |     static func f<X>(a: X) {}
   |                 ^ 
   | 

error: 'main' is missing
 ==> generic_func_conflict.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
