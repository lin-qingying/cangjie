// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkga.cj pkgb.cj 
// EXEC: %compiler %cmp_opt %compile_lib_opt %enableO2 pkga.cj -o pkga.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt %enableO2 pkgb.cj -o pkgb.%middle
// (CJNATIVE) EXEC: %compiler %cmp_opt %enableO2 %f pkga.%middle pkgb.%middle -o %output 2>&1 | compare %f

package pkg

import pkg.a.ValueType as AValueType
import pkg.b.ValueType as BValueType

main() {}

/* SCAN
warning: unused import 'pkg.a.ValueType as AValueType'
  ==> test.cj:14:1:
   | 
14 | import pkg.a.ValueType as AValueType
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused import 'pkg.b.ValueType as BValueType'
  ==> test.cj:15:1:
   | 
15 | import pkg.b.ValueType as BValueType
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

error: interface property 'code' must be implemented in 'extend Int64'
  ==> (package pkg.a)pkga.cj:21:1:

error: interface property 'code' must be implemented in 'extend Int64'
  ==> (package pkg.b)pkgb.cj:21:1:

2 errors generated, 2 errors printed.
2 warnings generated, 2 warnings printed.
*/
