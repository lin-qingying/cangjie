// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../../apilevel.cj dep2.cj dep2.cj.d
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.%middle dep2.cj -o dep2.%middle
// ERRCHECK: %compiler %cmp_opt %f dep2.%middle apilevel.%middle --cfg=APILevel_level=12

import dep2.*

main() {
   println(aaaa)
   println(bbbb)
   let a = cccc
}

/* SCAN
error: cannot reference 'aaaa'(level: 13) which higher than level of the current scope(level: 12)
  ==> merge3.cj:15:12:
   | 
15 |    println(aaaa)
   |            ^^^^ 
   | 

error: cannot reference 'bbbb'(level: 13) which higher than level of the current scope(level: 12)
  ==> merge3.cj:16:12:
   | 
16 |    println(bbbb)
   |            ^^^^ 
   | 

error: cannot reference 'cccc'(level: 13) which higher than level of the current scope(level: 12)
  ==> merge3.cj:17:12:
   | 
17 |    let a = cccc
   |            ^^^^ 
   | 

3 errors generated, 3 errors printed.
*/
