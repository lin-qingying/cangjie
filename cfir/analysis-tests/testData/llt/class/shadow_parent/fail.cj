// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga.cj -o pkga.%middle
// ERRCHECK: %compiler %cmp_opt pkga.%middle %f -o %output

import pkga.*

class B <: A {
    func f() {
        1
    }
}

main() {
    B().f()
}

/* SCAN
error: cannot override function 'f'
  ==> fail.cj:14:10:
   | 
14 |     func f() {
   |          ^ 
   | 
note: the parent function is not modified by 'open'
  ==> (package pkga)pkga.cj:10:17:

1 error generated, 1 error printed.
*/
