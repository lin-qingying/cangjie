// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg/pkg.cj -o pkg.%middle
// EXEC-PIPE-1: %compiler %cmp_opt pkg.%middle %n.cj -o %n.%suffix 2>&1 | compare %f

internal import pkg.*

type C = A

type D = B

extend C <: D {}

interface E <: D {}

extend A1 <: E {}

main() {
    return 0
}

/* SCAN
error: imported type 'A' cannot extend imported interface
  ==> main.cj:17:8:
   | 
17 | extend C <: D {}
   |        ^ 
   | 
   # note: used external interface: Interface-B

error: imported type 'A1' cannot extend imported interface
  ==> main.cj:21:8:
   | 
21 | extend A1 <: E {}
   |        ^^ 
   | 
   # note: used external interface: Interface-B

2 errors generated, 2 errors printed.
*/
