// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    {=> a}()
    var a = 0
    return 0
}

/* SCAN
error: undeclared identifier 'a'
  ==> capture_after.cj:10:9:
   | 
10 |     {=> a}()
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
