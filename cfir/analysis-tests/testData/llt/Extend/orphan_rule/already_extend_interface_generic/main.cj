// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg/pkg.cj -o pkg.%middle
// EXEC-PIPE-1: %compiler %cmp_opt pkg.%middle %n.cj -o %n.%suffix 2>&1 | compare %f

internal import pkg.*

interface II <: I<Int8> {}

extend C <: II {}

interface III <: I<Int64> {}

extend C <: III {}

main() {
    0
}

/* SCAN
error: imported type 'C' cannot extend imported interface
  ==> main.cj:15:8:
   | 
15 | extend C <: II {}
   |        ^ 
   | 
   # note: used external interface: Interface-I<Int8>

1 error generated, 1 error printed.
*/
