// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ../src
// EXEC: %compiler %cmp_opt %compile_lib_opt src/pkgs/pkgs.cj -o pkgs.%middle
// EXEC: %compiler %cmp_opt pkgs.%middle %f -o %output 2>&1 |compare %f

import pkgs.*

main(): Int64 {
    obj.foo()
    return 1
}

/* SCAN
error: cannot use mutable function on immutable value
  ==> main.cj:14:5:
   | 
14 |     obj.foo()
   |     ^^^ ~~~ is a mutable function
   |     |
   |     is immutable
   | 
note: 'obj' is a variable declared with 'let'
  ==> (package pkgs)pkgs.cj:19:12:

1 error generated, 1 error printed.
*/
