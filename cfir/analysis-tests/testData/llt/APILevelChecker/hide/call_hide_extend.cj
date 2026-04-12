// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj call_hide_extend_dep.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj -o hide.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt call_hide_extend_dep.cj -o call_hide_extend_dep.%middle
// ERRCHECK: %compiler %cmp_opt hide.%middle call_hide_extend_dep.%middle %n.cj -o %n.%suffix -Woff all

import ohos.dep.*

main() {
    A().foo() // ok
    A().goo() // error
    A().koo() // error
}

/* SCAN
error: undeclared identifier 'goo'
  ==> call_hide_extend.cj:16:5:
   | 
16 |     A().goo() // error
   |     ^^^^^^^^^ 
   | 

error: undeclared identifier 'koo'
  ==> call_hide_extend.cj:17:5:
   | 
17 |     A().koo() // error
   |     ^^^^^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
