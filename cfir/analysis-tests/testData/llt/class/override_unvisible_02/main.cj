// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// ERRCHECK: %compiler %cmp_opt pkg.%middle %n.cj -o %output

import pkg.*

extend B {
    func ff() {
        3
    } // f3. conflict 
}

main() {
    let v = B().ff()
    if (v != 3) {
        return 1
    }
    return 0
}

/* SCAN
error: extend member 'ff' is not allowed to shadow members of 'Class-B'
  ==> main.cj:14:10:
   | 
14 |     func ff() {
   |          ^^ 
   | 
note: shadowed definition of 'ff' is here
  ==> (package pkg)pkg.cj:16:20:

1 error generated, 1 error printed.
*/
