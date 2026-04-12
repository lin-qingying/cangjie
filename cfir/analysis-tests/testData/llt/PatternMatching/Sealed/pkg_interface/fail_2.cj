// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkgs.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkgs.cj -o pkgs.%middle
// ERRCHECK: %compiler %cmp_opt pkgs.%middle %f -o %output

import pkgs.*

struct A <: I {}

main(): Int64 {
    return A.f() - 1
}

/* SCAN
error: cannot inherit imported 'sealed' interface 'I'
  ==> fail_2.cj:13:8:
   | 
13 | struct A <: I {}
   |        ^    ~ sealed interface declared in package 'pkgs'
   | 

1 error generated, 1 error printed.
*/
