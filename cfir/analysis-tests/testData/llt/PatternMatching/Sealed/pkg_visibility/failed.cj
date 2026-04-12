// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// ERRCHECK: %compiler %cmp_opt pkg.%middle %f -o %output

internal import pkg.*

func f(x: I) {
    match (x) {
        case _: A => 0
        case _: B => 1
    }
}

main() {
    f(A())
}

/* SCAN
error: undeclared type name 'B'
  ==> failed.cj:16:17:
   | 
16 |         case _: B => 1
   |                 ^ 
   | 

error: non-exhaustive patterns
  ==> failed.cj:14:12:
   | 
14 |     match (x) {
   |            ^ the selector is of type 'Interface-I'
   | 
   # note: _ is not covered

2 errors generated, 2 errors printed.
*/
