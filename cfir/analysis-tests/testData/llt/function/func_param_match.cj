// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

func foo(a: Float32) {
    0
}

main() {
    let a: Float32 = 1
    return foo(1)
}

/* SCAN
error: cannot convert an integer literal to type 'Float32'
  ==> func_param_match.cj:14:22:
   | 
14 |     let a: Float32 = 1
   |                      ^ 
   | 

error: cannot convert an integer literal to type 'Float32'
  ==> func_param_match.cj:15:16:
   | 
15 |     return foo(1)
   |                ^ 
   | 

2 errors generated, 2 errors printed.
*/
