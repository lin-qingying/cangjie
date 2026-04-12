// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: m1
// DEPENDENCE: m2
// EXEC: %compiler %cmp_opt %compile_lib_opt %mod_name_opt m1 ./m1/p1.cj -o m1/p1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt %mod_name_opt m2 ./m2/p1.cj -o m2/p1.%middle
// ERRCHECK: %compiler %cmp_opt m1/p1.%middle m2/p1.%middle %f -o %output

import m1.p1

main() {
    "${p1.g()}"
    0
}

/* SCAN
error: 'g' is not a member of package 'm1.p1'
  ==> err_not_member.cj:16:11:
   | 
16 |     "${p1.g()}"
   |           ^ 
   | 

1 error generated, 1 error printed.
*/
