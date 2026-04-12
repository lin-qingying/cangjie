// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

var f: (Int64) -> Int64 = {i: ToString => 1}

main() {}

/* SCAN
error: mismatched types
 ==> lambda11.cj:9:27:
  | 
9 | var f: (Int64) -> Int64 = {i: ToString => 1}
  |                           ^^^^^^^^^^^^^^^^^^ expected '(Int64) -> Int64', found '(Interface-ToString) -> Int64'
  | 

1 error generated, 1 error printed.
*/
