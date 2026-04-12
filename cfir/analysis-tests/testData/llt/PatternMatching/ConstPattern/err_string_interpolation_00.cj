// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(x: String) {
    match (x) {
        case "${x.size}" => 0
        case _ => 1
    }
}

main() {
    f("1")
}

/* SCAN
error: cannot use string interpolation in constant pattern
  ==> err_string_interpolation_00.cj:11:14:
   | 
11 |         case "${x.size}" => 0
   |              ^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
