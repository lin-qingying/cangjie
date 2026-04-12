// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../../apilevel.cj dep1.cj dep1.cj.d
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.%middle dep1.cj -o dep1.%middle
// ERRCHECK: %compiler %cmp_opt %f dep1.%middle apilevel.%middle --cfg=APILevel_level=2

import dep1.*

main() {
    A()
    B()
    C.A.foo()
    C.B(1).foo()
}

/* SCAN
error: cannot reference 'A'(level: 4) which higher than level of the current scope(level: 2)
  ==> merge2.cj:15:5:
   | 
15 |     A()
   |     ^^^ 
   | 

error: cannot reference 'B'(level: 15) which higher than level of the current scope(level: 2)
  ==> merge2.cj:16:5:
   | 
16 |     B()
   |     ^^^ 
   | 

error: cannot reference 'C'(level: 26) which higher than level of the current scope(level: 2)
  ==> merge2.cj:17:5:
   | 
17 |     C.A.foo()
   |     ^^^^^^^^^ 
   | 

error: cannot reference 'C'(level: 26) which higher than level of the current scope(level: 2)
  ==> merge2.cj:18:5:
   | 
18 |     C.B(1).foo()
   |     ^^^^^^^^^^^^ 
   | 

4 errors generated, 4 errors printed.
*/
