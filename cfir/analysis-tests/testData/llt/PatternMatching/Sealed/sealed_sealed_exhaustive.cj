// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

sealed interface Color {}

class Red <: Color {}

class Green <: Color {}

abstract sealed class Blue <: Color {}

class DarkBlue <: Blue {}

func f(c: Color) {
    match (c) {
        case _: Red => 0
        case _: Green => 1
        case _: DarkBlue => 2
    }
}

main() {
    f(Red())
}

/* SCAN
error: non-exhaustive patterns
  ==> sealed_sealed_exhaustive.cj:20:12:
   | 
20 |     match (c) {
   |            ^ the selector is of type 'Interface-Color'
   | 
   # note: _ is not covered

1 error generated, 1 error printed.
*/
