// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

enum E {
    | A
    | B
}

enum F {
    | A
    | B
}

main() {
    let x = E.A
    match (x) {
        case A => 0
        case F.B => 1
    }
}

/* SCAN
error: enum pattern is not matched
  ==> usertype_01.cj:23:14:
   | 
23 |         case F.B => 1
   |              ^ 
   | 

1 error generated, 1 error printed.
*/
