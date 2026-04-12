// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: unused013pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt unused013pkga.cj -o pkga.%middle
// EXEC-PIPE: %compiler %cmp_opt pkga.%middle %f -o %output 2>&1 | compare %f

import pkga.mymap as mymapanew
import pkga.pkghash as pkghashanew
import pkga.*

main(): Int64 {
    var a : HashMap<String, Int64> = mymap(10)
    return a.size
}

/* SCAN
warning: unused import 'pkga.mymap as mymapanew'
  ==> unused013.cj:11:1:
   | 
11 | import pkga.mymap as mymapanew
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused import 'pkga.pkghash as pkghashanew'
  ==> unused013.cj:12:1:
   | 
12 | import pkga.pkghash as pkghashanew
   | ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/

