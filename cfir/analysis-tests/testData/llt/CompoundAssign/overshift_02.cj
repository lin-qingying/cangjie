// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var a: Int8 = 1
    a <<= 8
    return 0
}

/* SCAN
error: shift count overflow
  ==> overshift_02.cj:11:11:
   | 
11 |     a <<= 8
   |           ^ 
   | 

1 error generated, 1 error printed.
*/
