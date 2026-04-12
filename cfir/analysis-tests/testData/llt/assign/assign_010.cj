// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let myLetUInt8: UInt8 = 0
    if (true) {
        print("${myLetUInt8}")
    } else {
        myLetUInt8 = 12
    }
}

/* SCAN
error: cannot assign to immutable value
  ==> assign_010.cj:14:9:
   | 
14 |         myLetUInt8 = 12
   |         ^^^^^^^^^^^^^^^ 
   | 
note: variable 'myLetUInt8' is immutable
  ==> assign_010.cj:10:9:
   | 
10 |     let myLetUInt8: UInt8 = 0
   |         ^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
