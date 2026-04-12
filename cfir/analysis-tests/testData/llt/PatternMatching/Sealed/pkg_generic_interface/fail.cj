// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// ERRCHECK: %compiler %cmp_opt pkg.%middle %f -o %output

internal import pkg.I

extend<T> Array<T> <: I<T> {}

main() {}

/* SCAN
error: imported type 'Array' cannot extend imported interface
  ==> fail.cj:13:11:
   | 
13 | extend<T> Array<T> <: I<T> {}
   |           ^^^^^^^^ 
   | 
   # note: used external interface: Interface-I<Generics-T>

1 error generated, 1 error printed.
*/
