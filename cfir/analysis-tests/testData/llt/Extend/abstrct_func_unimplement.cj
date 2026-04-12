// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I {
    static func f(): Int64
}

struct C {}

extend C <: I {}

/* SCAN
error: implementation of function or property is needed in 'extend C'
  ==> abstrct_func_unimplement.cj:15:1:
   | 
15 | extend C <: I {}
   | ^ 
   | 
note: unimplemented interface function 'f'
  ==> abstrct_func_unimplement.cj:10:5:
   | 
10 |     static func f(): Int64
   |     ^ 
   | 

error: 'main' is missing
 ==> abstrct_func_unimplement.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
