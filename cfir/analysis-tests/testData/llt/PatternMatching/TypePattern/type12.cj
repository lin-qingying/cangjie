// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main() {
    match ((1, 1)) {
        case (x: Int64, x: Int64) => ()
        case (_, _) => ()
    }
}

/* SCAN
error: redefinition of declaration 'x'
  ==> type12.cj:11:25:
   | 
11 |         case (x: Int64, x: Int64) => ()
   |                         ^ 
   | 
note: 'x' is previously declared here
  ==> type12.cj:11:15:
   | 
11 |         case (x: Int64, x: Int64) => ()
   |               ^ 
   | 

1 error generated, 1 error printed.
*/
