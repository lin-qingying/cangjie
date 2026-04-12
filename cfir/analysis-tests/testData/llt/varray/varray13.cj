// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-0: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC-PIPE-0: %run %run_opt %output %run_args

func foo() {
    var i = 0
    while (i < 5) {
        i++
        var arr1: VArray<Nothing, $5> = VArray<Nothing, $5>(repeat: continue)
    }
    return 0
}

main() {
    return foo()
}

/* SCAN
warning: unreachable expression
  ==> varray13.cj:14:9:
   | 
14 |         var arr1: VArray<Nothing, $5> = VArray<Nothing, $5>(repeat: continue)
   |         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^any code following this expression is unreachable
   |         |
   |         unreachable expression
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused variable:'arr1'
  ==> varray13.cj:14:13:
   | 
14 |         var arr1: VArray<Nothing, $5> = VArray<Nothing, $5>(repeat: continue)
   |             ^^^^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/
