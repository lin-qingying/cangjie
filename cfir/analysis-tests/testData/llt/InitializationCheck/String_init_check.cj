// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    var str1: String
    var str2 = str1 + "4"
    print(str2)
    return 0
}

/* SCAN
error: variable 'str1' is used before initialization
  ==> String_init_check.cj:11:16:
   | 
11 |     var str2 = str1 + "4"
   |                ^ 
   | 

1 error generated, 1 error printed.
*/
