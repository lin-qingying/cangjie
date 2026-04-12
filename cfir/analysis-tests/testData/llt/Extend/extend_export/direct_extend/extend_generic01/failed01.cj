// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt a.cj -o a.%middle
// EXEC-PIPE-1: %compiler %cmp_opt a.%middle %f -o %output 2>&1 | compare %f

import a.A

main() {
    A<Int64>().foo()
}

/* SCAN
error: 'foo' is not a member of class 'A<Int64>'
  ==> failed01.cj:14:16:
   | 
14 |     A<Int64>().foo()
   |                ^^^ 
   | 
note: to use the following extension, import 'Interface-Upper'
  ==> (package a)a.cj:15:1:

1 error generated, 1 error printed.
*/
