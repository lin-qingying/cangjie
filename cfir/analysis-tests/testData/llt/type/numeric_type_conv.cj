// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

main() {
    let a = UInt64(0xFFFF_FFFF_FFFF_FFFF)
    a
    0
}

/* SCAN
error: the number '0xFFFF_FFFF_FFFF_FFFF' exceeds the value range of type 'Int64'
  ==> numeric_type_conv.cj:10:20:
   | 
10 |     let a = UInt64(0xFFFF_FFFF_FFFF_FFFF)
   |                    ^^^^^^^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
