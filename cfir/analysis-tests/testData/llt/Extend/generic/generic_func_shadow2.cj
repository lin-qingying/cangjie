// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class C<A> {
    func f<X>(a: A, b: X) {}
    static func g<X>(a: A, b: X) {}
}

extend<B> C<B> {
    func f<Y>(a: B, b: Y) {}
    static func g<Y>(c: B, d: Y) {}
}

/* SCAN
error: extend member 'f' is not allowed to shadow members of 'Class-C<Generics-B>'
  ==> generic_func_shadow2.cj:15:10:
   | 
15 |     func f<Y>(a: B, b: Y) {}
   |          ^ 
   | 
note: shadowed definition of 'f' is here
  ==> generic_func_shadow2.cj:10:10:
   | 
10 |     func f<X>(a: A, b: X) {}
   |          ^ 
   | 

error: extend member 'g' is not allowed to shadow members of 'Class-C<Generics-B>'
  ==> generic_func_shadow2.cj:16:17:
   | 
16 |     static func g<Y>(c: B, d: Y) {}
   |                 ^ 
   | 
note: shadowed definition of 'g' is here
  ==> generic_func_shadow2.cj:11:17:
   | 
11 |     static func g<X>(a: A, b: X) {}
   |                 ^ 
   | 

error: 'main' is missing
 ==> generic_func_shadow2.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
