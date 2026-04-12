// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A<T> {}

extend<T> A<T> {
    public static func f() {}
    public static func f() {}
}

/* SCAN
error: function 'f' has overload conflicts
  ==> func_conflict.cj:13:24:
   | 
13 |     public static func f() {}
   |                        ^ 
   | 
note: conflict with the declaration
  ==> func_conflict.cj:12:24:
   | 
12 |     public static func f() {}
   |                        ^ 
   | 

error: 'main' is missing
 ==> func_conflict.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
