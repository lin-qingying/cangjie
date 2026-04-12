// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg0.cj pkg1.cj pkg2.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg0.cj -o pkg0.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg0.%middle pkg1.cj -o pkg1.%middle
// ERRCHECK: %compiler %cmp_opt %compile_lib_opt pkg0.%middle pkg1.%middle pkg2.cj -o pkg2.%middle 2>&1

package pkg2

internal import pkg0.A
internal import pkg1.I

public interface K {}

extend A <: K {
    public func fk() {
        this.fj() + 1
    } // pkg1.J not import, cannot access 'fj'.
}

/* SCAN
error: no matching function declaration for function call 'fj'
  ==> pkg2.cj:21:14:
   | 
21 |         this.fj() + 1
   |              ^ 
   | 
note: to use the following extension, import 'Interface-J'
  ==> (package pkg1)pkg1.cj:19:1:

1 error generated, 1 error printed.
*/
