// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    var str1 = "cangjie"
    var str2 = "CangJie"
    var res1: String
    let iterable = str1
    for (i in iterable) {
        res1 = res1 + i.toString()
    }

    if (res1 == "228187147233162137") {
        return 0
    }

    return 1
}

/* SCAN
error: variable 'res1' is used before initialization
  ==> String_init_check2.cj:15:16:
   | 
15 |         res1 = res1 + i.toString()
   |                ^ 
   | 

error: variable 'res1' is used before initialization
  ==> String_init_check2.cj:18:9:
   | 
18 |     if (res1 == "228187147233162137") {
   |         ^ 
   | 

2 errors generated, 2 errors printed.
*/
