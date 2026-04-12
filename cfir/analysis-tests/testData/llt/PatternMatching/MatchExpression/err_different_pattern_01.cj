// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

enum E {
    e
}

func f(x: Bool) {
    match (x) {
        case true | e => 0
    }
}

main() {}

/* SCAN
error: patterns connected by '|' should be of the same kind
  ==> err_different_pattern_01.cj:15:14:
   | 
15 |         case true | e => 0
   |              ^^^^   ~ enum pattern
   |              |
   |              constant pattern
   | 

1 error generated, 1 error printed.
*/
