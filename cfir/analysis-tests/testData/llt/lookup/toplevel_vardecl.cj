// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

let a = b
let b = a

/* SCAN
error: unable to infer declaration type, please add type annotation
 ==> toplevel_vardecl.cj:9:5:
  | 
9 | let a = b
  |     ^ 
  | 

error: variable 'b' is used before being defined
 ==> toplevel_vardecl.cj:9:9:
  | 
9 | let a = b
  |         ^ 
  | 

2 errors generated, 2 errors printed.
*/
