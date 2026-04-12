// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: testa.cj
// EXEC: %compiler %cmp_macro testa.cj
// ERRCHECK: %compiler %noColor %f -o %output

import define_10004.*

main() {
    var a = quote(1+1) // empty lambda
    a.dump()
    return 0
}

/* SCAN
error: import 'std.ast' to use the 'quote' expression
  ==> main_err.cj:14:13:
   | 
14 |     var a = quote(1+1) // empty lambda
   |             ^^^^^^^^^^ 
   | 

warning: unused import 'define_10004.*'
  ==> main_err.cj:11:1:
   | 
11 | import define_10004.*
   | ^^^^^^^^^^^^^^^^^^^^^ unused import
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

1 error generated, 1 error printed.
1 warning generated, 1 warning printed.
*/
