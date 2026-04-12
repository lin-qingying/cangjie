// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f


func f() {
    enum E{e}
    struct R{var r: Int32}
}

/* SCAN
error: unexpected enum declaration in function body
  ==> composite_type.cj:11:5:
   | 
10 | func f() {
   | ~~~~ in function
11 |     enum E{e}
   |     ^^^^ unexpected enum declaration
   | 

error: unexpected struct declaration in function body
  ==> composite_type.cj:12:5:
   | 
10 | func f() {
   | ~~~~ in function
11 |     enum E{e}
12 |     struct R{var r: Int32}
   |     ^^^^^^ unexpected struct declaration
   | 

2 errors generated, 2 errors printed.
*/
