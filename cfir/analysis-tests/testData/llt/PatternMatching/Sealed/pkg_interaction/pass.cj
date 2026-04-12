// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// ERRCHECK: %compiler %cmp_opt pkg.%middle %f -o %output

internal import pkg.*

func mediator(f: Interaction) {
    match (f) {
        case _: Weak => "W and Z bosons"
        case _: Strong => "gluons"
        case _: Gravity => "gravitons"
        case _: Electromagnetic => "photons"
    }
}

main() {
    println(mediator(Gravity()))
}

/* SCAN
error: non-exhaustive patterns
  ==> pass.cj:14:12:
   | 
14 |     match (f) {
   |            ^ the selector is of type 'Interface-Interaction'
   | 
   # note: _ is not covered

1 error generated, 1 error printed.
*/
