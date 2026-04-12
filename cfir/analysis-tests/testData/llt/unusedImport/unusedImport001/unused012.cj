// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: unused012pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt unused012pkga.cj -o pkga.%middle
// EXEC-PIPE: %compiler %cmp_opt pkga.%middle %f -o %output 2>&1 | compare %f

import pkga.test
import pkga.test as testanew
main(): Int64 {
    testanew()
    return 0
}

/* SCAN
warning: unused import 'pkga.test'
  ==> unused012.cj:11:1:
   | 
11 | import pkga.test
   | ^^^^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

1 warning generated, 1 warning printed.
*/

