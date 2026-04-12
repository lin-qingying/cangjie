// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    var x = 0

    const init() {}
}

main() {}

/* SCAN
error: cannot define 'const' constructor with 'var' members in class
  ==> err_class_var.cj:12:11:
   | 
12 |     const init() {}
   |           ^^^^ 
   | 
note: member variable declared with 'var'
  ==> err_class_var.cj:10:9:
   | 
10 |     var x = 0
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
