// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func A() {
    {i => i}
    {i, j => i + j}
}

main() {
    return 0
}

/* SCAN
error: parameters of this lambda expression must have type annotations
  ==> lambda04.cj:10:6:
   | 
10 |     {i => i}
   |      ^ 
   | 

error: parameters of this lambda expression must have type annotations
  ==> lambda04.cj:11:6:
   | 
11 |     {i, j => i + j}
   |      ^ 
   | 

2 errors generated, 2 errors printed.
*/
