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
    foo() // level 1
    foo([1]) // level 2
    foo(["1"]) // level 3
}

/* SCAN
error: cannot reference 'foo'(level: 3) which higher than level of the current scope(level: 2)
  ==> merge1.cj:18:5:
   | 
18 |     foo(["1"]) // level 3
   |     ^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
