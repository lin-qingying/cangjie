// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj call_hide_param_dep.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj -o hide.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt call_hide_param_dep.cj -o call_hide_param_dep.%middle
// ERRCHECK: %compiler %cmp_opt %compile_lib_opt call_hide_param_dep.%middle hide.%middle %n.cj -o %n.%suffix -Woff all

import ohos.dep.*

main() {
    A().a
}

/* SCAN
error: undeclared identifier 'a'
  ==> call_hide_param.cj:15:5:
   | 
15 |     A().a
   |     ^^^^^ 
   | 

1 error generated, 1 error printed.
*/
