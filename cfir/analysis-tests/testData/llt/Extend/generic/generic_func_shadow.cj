// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class C {
    func f<T>(a: T) {}
}

extend C {
    func f<V>(a: V) {}
}

/* SCAN
error: extend member 'f' is not allowed to shadow members of 'Class-C'
  ==> generic_func_shadow.cj:14:10:
   | 
14 |     func f<V>(a: V) {}
   |          ^ 
   | 
note: shadowed definition of 'f' is here
  ==> generic_func_shadow.cj:10:10:
   | 
10 |     func f<T>(a: T) {}
   |          ^ 
   | 

error: 'main' is missing
 ==> generic_func_shadow.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
