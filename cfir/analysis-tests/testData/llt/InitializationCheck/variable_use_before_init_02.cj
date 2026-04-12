// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    var a: Int64
    var b: Int64
    if ((1, b) == (1, 2)) {
        print(1)
    }
    if (b == 1) {
        a = 1
    }
    return b - a - a
}

/* SCAN
error: variable 'b' is used before initialization
  ==> variable_use_before_init_02.cj:12:13:
   | 
12 |     if ((1, b) == (1, 2)) {
   |             ^ 
   | 

error: variable 'b' is used before initialization
  ==> variable_use_before_init_02.cj:15:9:
   | 
15 |     if (b == 1) {
   |         ^ 
   | 

error: variable 'b' is used before initialization
  ==> variable_use_before_init_02.cj:18:12:
   | 
18 |     return b - a - a
   |            ^ 
   | 

3 errors generated, 3 errors printed.
*/
