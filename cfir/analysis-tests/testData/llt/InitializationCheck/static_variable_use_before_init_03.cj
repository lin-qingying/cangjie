// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    static var a: Int64
    static var b: Int64
    static init() {
        a = 1
    }
}

main() {
}

/* SCAN
error: the static member variable 'b' is not initialized
  ==> static_variable_use_before_init_03.cj:11:5:
   | 
11 |     static var b: Int64
   |     ^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: you may add an initializer inline or initialize the variable in the 'static init'
note: static init defined here
  ==> static_variable_use_before_init_03.cj:12:12:
   | 
12 |     static init() {
   |            ^^^^ 
   | 

1 error generated, 1 error printed.
*/
