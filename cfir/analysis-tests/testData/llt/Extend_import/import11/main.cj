// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj pkg1.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt %enableO2 pkg1.cj -o pkg1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt %enableO2 pkg.cj pkg1.%middle -o pkg.%middle
// EXEC: %compiler %cmp_opt %enableO2 %f pkg.%middle pkg1.%middle -o %output 2>&1 | compare %f

internal import pkg1.*

internal import pkg.*

interface I2<T> <: I<T> {}

extend<T3> A<T3> <: I2<T3> {}

extend<T3> B<T3> <: I2<T3> {}

main() {}

/* SCAN
error: imported type 'B' cannot extend imported interface
  ==> main.cj:20:12:
   | 
20 | extend<T3> B<T3> <: I2<T3> {}
   |            ^^^^^ 
   | 
   # note: used external interface: Interface-I<Generics-T3>

1 error generated, 1 error printed.
*/
