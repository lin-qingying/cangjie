// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    let b = [[1u8, 2u8, 3u8]]
    b[1.01][0] != 1
    return 0
}

/* SCAN
error: cannot convert a floating-point literal to type 'Int64'
  ==> subscribe_in_binary.cj:11:7:
   | 
11 |     b[1.01][0] != 1
   |       ^ 
   | 

error: parameters and arguments mismatch
  ==> subscribe_in_binary.cj:11:5:
   | 
11 |     b[1.01][0] != 1
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
