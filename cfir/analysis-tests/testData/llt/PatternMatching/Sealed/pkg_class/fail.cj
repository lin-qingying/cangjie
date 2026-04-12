// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga.cj -o pkga.%middle
// ERRCHECK: %compiler %cmp_opt pkga.%middle %f -o %output

internal import pkga.*

class C <: A {} // error

func f(x: A) {
    match (x) {
        case b: B => b.f()
        case a: A => a.f()
    }
}

main() {
    f(B())
}

/* SCAN
error: cannot inherit imported 'sealed' class 'A'
  ==> fail.cj:13:7:
   | 
13 | class C <: A {} // error
   |       ^    ~ sealed class declared in package 'pkga'
   | 

1 error generated, 1 error printed.
*/
