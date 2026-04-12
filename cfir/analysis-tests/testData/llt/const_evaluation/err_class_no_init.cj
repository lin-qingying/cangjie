// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    static const zero = 0
    const func f() {
        zero
    }
}

main() {
    let a = A()
    a.f()
}

/* SCAN
error: cannot define 'const' member function without 'const' constructor
  ==> err_class_no_init.cj:11:16:
   | 
11 |     const func f() {
   |                ^ 
   | 
note: consider add a 'const' constructor to this class
 ==> err_class_no_init.cj:9:7:
  | 
9 | class A {
  |       ^ 
  | 

1 error generated, 1 error printed.
*/
