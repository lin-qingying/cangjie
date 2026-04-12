// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../../apilevel.cj dep.cj dep.cj.d
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.%middle dep.cj -o dep.%middle
// ERRCHECK: %compiler %cmp_opt %f dep.%middle apilevel.%middle --cfg=APILevel_level=11

import dep.*

main() {
    foo((1, 1))(1)
    0
}

/* SCAN
error: cannot reference 'foo'(level: 12) which higher than level of the current scope(level: 11)
  ==> test.cj:15:5:
   | 
15 |     foo((1, 1))(1)
   |     ^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
