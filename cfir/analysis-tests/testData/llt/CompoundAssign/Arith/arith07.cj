// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    var a: Int64
    a **= 2
    return 0
}

/* SCAN
error: variable 'a' is used before initialization
  ==> arith07.cj:11:5:
   | 
11 |     a **= 2
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
