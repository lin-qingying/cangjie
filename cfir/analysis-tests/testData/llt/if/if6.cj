// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var a: Int32 = 1;
    let x: Int64 = if (false) {
        2;
    } else {
        a;
    };
    return 0;
}

/* SCAN
error: mismatched types
  ==> if6.cj:14:9:
   | 
14 |         a;
   |         ^ expected 'Int64', found 'Int32'
   | 

1 error generated, 1 error printed.
*/
