// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

public abstract class A {}

class B <: A {}

class C <: A {}

func f(x: A) {
    match (x) {
        case _: B => 0
        case _: C => 1
    }
}

main() {
    f(B())
}

/* SCAN
error: non-exhaustive patterns
  ==> non_exhaustive_public_01.cj:16:12:
   | 
16 |     match (x) {
   |            ^ the selector is of type 'Class-A'
   | 
   # note: _ is not covered

1 error generated, 1 error printed.
*/
