// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %n.cj -o %output

main() {
    let x = Some(true)
    match (x) {
        case Some(true) | false | 0 => 0
        case Some(false) => 1
        case _ => 2
    }
}

/* SCAN
error: patterns connected by '|' should be of the same kind
  ==> match023.cj:12:14:
   | 
12 |         case Some(true) | false | 0 => 0
   |              ^^^^^^^^^^   ~~~~~ constant pattern
   |              |
   |              enum pattern
   | 

1 error generated, 1 error printed.
*/
