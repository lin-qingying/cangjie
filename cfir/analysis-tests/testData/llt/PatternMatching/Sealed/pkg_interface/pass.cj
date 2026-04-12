// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga.cj -o pkga.%middle
// ERRCHECK: %compiler %cmp_opt pkga.%middle %f -o %output

internal import pkga.*

interface J {
    func g(): Int64
}

extend B <: J { // ok, sealed class can implement new interface
    public func g() {
        9
    }
}

func f(x: I) {
    match (x) {
        case c: B => c.g()
        case a: A => a.f()
    }
}

main() {
    f(C())
}

/* SCAN
error: non-exhaustive patterns
  ==> pass.cj:24:12:
   | 
24 |     match (x) {
   |            ^ the selector is of type 'Interface-I'
   | 
   # note: _ is not covered

1 error generated, 1 error printed.
*/
