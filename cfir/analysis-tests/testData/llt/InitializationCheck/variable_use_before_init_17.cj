// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f() {
    var x: Int64
    while (true) {
        x++
    }
}

main() {}

/* SCAN
error: variable 'x' is used before initialization
  ==> variable_use_before_init_17.cj:12:9:
   | 
12 |         x++
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
