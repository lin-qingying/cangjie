// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class C<T> {}

extend<A> C<A> {
    func f<X>(a: A, b: X) {}
    static func g<X>(a: A, b: X) {}
}

extend<B> C<B> {
    func f<Y>(a: B, b: Y) {}
    static func g<Y>(c: B, d: Y) {}
}

/* SCAN
error: extend member 'f' is not allowed to shadow members of 'extend C'
  ==> generic_func_conflict2.cj:12:10:
   | 
12 |     func f<X>(a: A, b: X) {}
   |          ^ 
   | 

error: extend member 'g' is not allowed to shadow members of 'extend C'
  ==> generic_func_conflict2.cj:13:17:
   | 
13 |     static func g<X>(a: A, b: X) {}
   |                 ^ 
   | 

error: extend member 'f' is not allowed to shadow members of 'extend C'
  ==> generic_func_conflict2.cj:17:10:
   | 
17 |     func f<Y>(a: B, b: Y) {}
   |          ^ 
   | 

error: extend member 'g' is not allowed to shadow members of 'extend C'
  ==> generic_func_conflict2.cj:18:17:
   | 
18 |     static func g<Y>(c: B, d: Y) {}
   |                 ^ 
   | 

error: 'main' is missing
 ==> generic_func_conflict2.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

5 errors generated, 5 errors printed.
*/
