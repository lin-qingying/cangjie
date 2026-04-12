// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var numOther = 18446744073709551615
    println(numOther is Int64)
    println(numOther)
    return 0
}

/* SCAN
error: the number '18446744073709551615' exceeds the value range of type 'Int64'
  ==> overflow_check_01.cj:10:20:
   | 
10 |     var numOther = 18446744073709551615
   |                    ^^^^^^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
