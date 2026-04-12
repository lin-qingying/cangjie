// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: unused014pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt unused014pkga.cj -o pkga.%middle
// EXEC-PIPE: %compiler %cmp_opt pkga.%middle %f -o %output 2>&1 | compare %f

import pkga.mymap as mymapanew
import pkga.pkghash as pkghashanew
import pkga.mymap
import pkga.pkghash

main(): Int64 {
    var a : mymapanew = pkghashanew(10)
    return a.size
}

/* SCAN
warning: unused import 'pkga.mymap'
  ==> unused014.cj:13:1:
   | 
13 | import pkga.mymap
   | ^^^^^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused import 'pkga.pkghash'
  ==> unused014.cj:14:1:
   | 
14 | import pkga.pkghash
   | ^^^^^^^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/

