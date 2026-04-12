// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga.cj -o pkga.%middle
// EXEC: %compiler %cmp_opt pkga.%middle %f -o %output 2>&1 | compare %f

internal import pkga.*

abstract sealed class C {
    public open func f() {
        0
    } // no warning
}

func f(x: A) {
    match (x) {
        case b: B => b.f()
    }
}

main() {
    f(B())
}

/* SCAN
error: non-exhaustive patterns
  ==> pass.cj:20:12:
   | 
20 |     match (x) {
   |            ^ the selector is of type 'Class-A'
   | 
   # note: _ is not covered

1 error generated, 1 error printed.
*/
