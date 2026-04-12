// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

abstract sealed class Color {}

class Red <: Color {}

class Green <: Color {}

class Blue <: Color {}

func f(c: Color) {
    match (c) { // Green, Blue not covered
        case _: Red => 0
    }
}

main() {
    f(Red())
}

/* SCAN
error: non-exhaustive patterns
  ==> simple_non_exhaustive.cj:18:12:
   | 
18 |     match (c) { // Green, Blue not covered
   |            ^ the selector is of type 'Class-Color'
   | 
   # note: _ is not covered

1 error generated, 1 error printed.
*/
