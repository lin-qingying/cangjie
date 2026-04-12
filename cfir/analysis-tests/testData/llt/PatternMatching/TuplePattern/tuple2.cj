// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    match (1) {
        case (k, v) => 1
        case _ => continue
    }
    0
}

/* SCAN
error: 'break' or 'continue' must be used inside a loop
  ==> tuple2.cj:12:19:
   | 
12 |         case _ => continue
   |                   ^ 
   | 

error: mismatched types
  ==> tuple2.cj:11:14:
   | 
11 |         case (k, v) => 1
   |              ^^^^^^ expected 'Int64', found 'Tuple'
   | 

2 errors generated, 2 errors printed.
*/
