// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    let t1 = (1, 2);
    if (t1 != (1, 2, 3)) {
        return 0;
    }
    return 1;
}

/* SCAN
error: operator '!=' between tuple type 'Tuple<Int64, Int64>' and 'Tuple<Int64, Int64, Int64>' is not supported
  ==> tupleEqual18.cj:11:9:
   | 
11 |     if (t1 != (1, 2, 3)) {
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
