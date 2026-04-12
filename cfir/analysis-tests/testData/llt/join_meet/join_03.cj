// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

internal import std.ast.*

main() {
    let x: String = if (true) {
        "a"
    } else {
        quote(1)
    }
}

/* SCAN
error: mismatched types
  ==> join_03.cj:15:9:
   | 
15 |         quote(1)
   |         ^^^^^^^^ expected 'Struct-String', found 'Class-Tokens'
   | 

1 error generated, 1 error printed.
*/
