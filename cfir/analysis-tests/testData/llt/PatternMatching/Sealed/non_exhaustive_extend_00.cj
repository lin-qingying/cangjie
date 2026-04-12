// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

sealed interface I {
    func f() {
        match (this) {
            case _: A => 1
        }
    }
}

extend Int64 <: I {}

class A <: I {}

main() {
    1.f()
}

/* SCAN
error: non-exhaustive patterns
  ==> non_exhaustive_extend_00.cj:11:16:
   | 
11 |         match (this) {
   |                ^^^^ the selector is of type 'Interface-I'
   | 
   # note: _ is not covered

1 error generated, 1 error printed.
*/
