// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main() {
    var v: Int64 = try {
        1
    } catch (e: Exception) {
        while (false) {}
    }
}

/* SCAN
error: mismatched types
  ==> while_check_type.cj:13:9:
   | 
13 |         while (false) {}
   |         ^^^^^^^^^^^^^^^^ expected 'Int64', found 'Unit'
   | 

1 error generated, 1 error printed.
*/
