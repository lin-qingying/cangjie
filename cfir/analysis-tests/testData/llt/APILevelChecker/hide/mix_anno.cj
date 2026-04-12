// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj ../level/apilevel.cj mix_anno_dep.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj apilevel.cj -o hide.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt mix_anno_dep.cj -o mix_anno_dep.%middle
// ERRCHECK: %compiler %cmp_opt mix_anno_dep.%middle hide.%middle %n.cj -o %n.%suffix -Woff all --cfg=APILevel_level=20

import ohos.dep.*

main() {
    A() // apilevel check error, hide check error.
    B() // apilevel check error, hide check error.
    0
}

/* SCAN
error: undeclared identifier 'A'
  ==> mix_anno.cj:15:5:
   | 
15 |     A() // apilevel check error, hide check error.
   |     ^^^ 
   | 

error: undeclared identifier 'B'
  ==> mix_anno.cj:16:5:
   | 
16 |     B() // apilevel check error, hide check error.
   |     ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
