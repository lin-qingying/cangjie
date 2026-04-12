// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
macro package define

internal import std.ast.*

public macro SumOfEachAbs(input: Tokens) {
    let x = parseExpr(input) | return quote($x)
    input
}

main() {}

/* SCAN
error: undeclared identifier 'x'
  ==> err_binary_00.cj:13:46:
   | 
13 |     let x = parseExpr(input) | return quote($x)
   |                                              ^ 
   | 

1 error generated, 1 error printed.
*/
