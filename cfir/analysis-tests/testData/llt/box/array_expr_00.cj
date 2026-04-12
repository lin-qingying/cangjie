// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %n.cj -o %output

main() {
    let x: Bool = Array<Int64>()
}

/* SCAN
error: mismatched types
  ==> array_expr_00.cj:10:19:
   | 
10 |     let x: Bool = Array<Int64>()
   |                   ^^^^^^^^^^^^^^ expected 'Bool', found 'Struct-Array<Int64>'
   | 

1 error generated, 1 error printed.
*/
