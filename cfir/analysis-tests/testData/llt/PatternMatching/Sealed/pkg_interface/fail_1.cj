// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga.cj -o pkga.%middle
// ERRCHECK: %compiler %cmp_opt pkga.%middle %f -o %output

internal import pkga.*

enum Color <: I { // error
    RED | GREEN | BLUE

    public func f() {
        2
    }
}

main() {}

/* SCAN
error: cannot inherit imported 'sealed' interface 'I'
  ==> fail_1.cj:13:6:
   | 
13 | enum Color <: I { // error
   |      ^^^^^    ~ sealed interface declared in package 'pkga'
   | 

1 error generated, 1 error printed.
*/
