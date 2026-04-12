// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {}

extend A {
    public static func f(a: Int8) {}
    public func f() {}
}

/* SCAN
error: overloaded functions 'f' cannot mix static and non-static
  ==> static_nonstatic_func_conflict.cj:12:24:
   | 
12 |     public static func f(a: Int8) {}
   |                        ^ 
   | 
note: non-static function is here
  ==> static_nonstatic_func_conflict.cj:13:17:
   | 
13 |     public func f() {}
   |                 ^ 
   | 

error: 'main' is missing
 ==> static_nonstatic_func_conflict.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
