// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj ../level/apilevel.cj mix_macro_dep.cj mix_macro_dep1.cj mix_macro2.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj apilevel.cj -o hide.%middle
// EXEC: %compiler %cmp_opt %cmp_macro mix_macro_dep.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt mix_macro_dep1.cj -o mix_macro_dep1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt mix_macro2.cj -o mix_macro2.%middle
// ERRCHECK: %compiler %cmp_opt %compile_lib_opt %n.cj -o %n.%suffix --cfg=APILevel_level=20

import ohos.dep.*

main() {
    A() // undefined
    B() // undefined
    C() // undefined
    D() // undefined
    0
}

/* SCAN
error: undeclared identifier 'A'
  ==> mix_macro2_use.cj:17:5:
   | 
17 |     A() // undefined
   |     ^^^ 
   | 

error: undeclared identifier 'B'
  ==> mix_macro2_use.cj:18:5:
   | 
18 |     B() // undefined
   |     ^^^ 
   | 

error: undeclared identifier 'C'
  ==> mix_macro2_use.cj:19:5:
   | 
19 |     C() // undefined
   |     ^^^ 
   | 

error: undeclared identifier 'D'
  ==> mix_macro2_use.cj:20:5:
   | 
20 |     D() // undefined
   |     ^^^ 
   | 

4 errors generated, 4 errors printed.
*/
