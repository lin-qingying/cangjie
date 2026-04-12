// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface I {
    const func f(): Int64
}

class A {}

extend A <: I {
    public const func f() {
        0
    }
}

main() {
    let a = A()
    a.f()
}

/* SCAN
error: cannot define 'const' member function without 'const' constructor
  ==> err_extend_no_const_init.cj:16:23:
   | 
16 |     public const func f() {
   |                       ^ 
   | 
note: consider add a 'const' constructor to this class
  ==> err_extend_no_const_init.cj:13:7:
   | 
13 | class A {}
   |       ^ 
   | 

1 error generated, 1 error printed.
*/
