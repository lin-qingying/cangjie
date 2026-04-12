// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    var a = VArray<Int64, $3>(repeat: 0)
    a.size()
    return 0
}

/* SCAN
error: no matching function for operator '()' function call
  ==> varray_size03.cj:11:5:
   | 
11 |     a.size()
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
