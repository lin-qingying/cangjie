// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main() {
    match (1) {
        case a@@b => 1  
    }
    return 0
}

/* SCAN
error: expected '=>' in case, found '@'
  ==> matchcase_fuzz_01.cj:11:15:
   | 
11 |         case a@@b => 1  
   |         ~~~~  ^ expected '=>' here
   | 

1 error generated, 1 error printed.
*/
