// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main(): Int64 {
    let val = {
        =>
        var a: Bool = false
        a
        return 1;
    }
    if (val) {
        return 1
    }
    return 0
}

/* SCAN
error: mismatched types
  ==> bottomType01.cj:16:9:
   | 
16 |     if (val) {
   |         ^^^ expected 'Bool', found '() -> Int64'
   | 

1 error generated, 1 error printed.
*/
