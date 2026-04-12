// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

let x = 0

main() {
    let (x, _) = match (x) {
        case _ => 0
    }
}

/* SCAN
error: initializer isn't a tuple to match tuple pattern
  ==> match.cj:12:9:
   | 
12 |     let (x, _) = match (x) {
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
