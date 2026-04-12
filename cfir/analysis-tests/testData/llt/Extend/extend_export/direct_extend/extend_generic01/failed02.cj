// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt a.cj -o a.%middle
// EXEC-PIPE-1: %compiler %cmp_opt a.%middle %f -o %output 2>&1 | compare %f

import a.Upper

main() {
    A<Int64>().foo()
}

/* SCAN
error: undeclared identifier 'A'
  ==> failed02.cj:14:5:
   | 
14 |     A<Int64>().foo()
   |     ^ 
   | 

warning: unused import 'a.Upper'
  ==> failed02.cj:11:1:
   | 
11 | import a.Upper
   | ^^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

1 error generated, 1 error printed.
1 warning generated, 1 warning printed.
*/
