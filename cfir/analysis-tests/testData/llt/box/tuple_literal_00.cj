// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %n.cj -o %output

main() {
    let x: Bool = (0, 0)
}

/* SCAN
error: mismatched types
  ==> tuple_literal_00.cj:10:19:
   | 
10 |     let x: Bool = (0, 0)
   |                   ^^^^^^ expected 'Bool', found 'Tuple'
   | 

1 error generated, 1 error printed.
*/
