// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main() {
    var slicing1 = Array<Bool>[3]
    Array<Bool>[3] = 1
    return 0
}

/* SCAN
error: '[]' is non-static member, cannot access by type name
  ==> array_access4.cj:10:20:
   | 
10 |     var slicing1 = Array<Bool>[3]
   |                    ^ 
   | 

error: '[]' is non-static member, cannot access by type name
  ==> array_access4.cj:11:5:
   | 
11 |     Array<Bool>[3] = 1
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
