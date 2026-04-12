// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj dep_ok.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt dep_ok.cj -o dep_ok.%middle
// ERRCHECK: %compiler %cmp_opt %compile_lib_opt %f apilevel.%middle dep_ok.%middle --cfg=APILevel_level=10

import dep.*

main() {
    f9()
    f11()
    f0()
}

/* SCAN
error: cannot reference 'f11'(level: 11) which higher than level of the current scope(level: 10)
  ==> test_dep01.cj:16:5:
   | 
16 |     f11()
   |     ^^^^^ 
   | 

1 error generated, 1 error printed.
*/
