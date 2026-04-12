// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(x: Bool) {
    match (x) {
        case true | x => 0
    }
}

main() {}

/* SCAN
error: cannot introduce variables in patterns connected by '|'
  ==> err_different_pattern_00.cj:11:21:
   | 
11 |         case true | x => 0
   |                     ^ 
   | 

1 error generated, 1 error printed.
*/
