// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../../apilevel.cj dep1.cj dep.cj.d
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.%middle dep1.cj -o dep.%middle
// ERRCHECK: %compiler %cmp_opt %f dep.%middle apilevel.%middle --cfg=APILevel_level=11

import dep.*

main() {
    A()
    foo()
    0
}

/* SCAN
error: cannot reference 'A'(level: 12) which higher than level of the current scope(level: 11)
  ==> test1.cj:15:5:
   | 
15 |     A()
   |     ^^^ 
   | 

error: cannot reference 'foo'(level: 12) which higher than level of the current scope(level: 11)
  ==> test1.cj:16:5:
   | 
16 |     foo()
   |     ^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
