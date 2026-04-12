// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%compiler %noColor %n.cj -o %output 2>&1 | compare %f

func f(): Option<Int64> {
    return Int32(4)
}

main(): Int64 {
    f()
    return 0
}

/* SCAN
error: mismatched types
  ==> numeric_type_conv_2.cj:10:12:
   | 
10 |     return Int32(4)
   |            ^^^^^^^^ expected 'Enum-Option<Int64>', found 'Int32'
   | 

1 error generated, 1 error printed.
*/
