// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC:%compiler %cmp_opt %compile_lib_opt pkg/pkg.cj -o pkg/pkg.%middle
// EXEC:%compiler %cmp_opt pkg/pkg.%middle %n.cj --import-path pkg -o %output 2>&1 | compare %f

internal import pkg.*

main(): Int64 {
    var x: Int64 = I.x()
    return 0
}

/* SCAN
error: undeclared identifier 'I'
  ==> main.cj:14:20:
   | 
14 |     var x: Int64 = I.x()
   |                    ^ 
   | 

1 error generated, 1 error printed.
*/
