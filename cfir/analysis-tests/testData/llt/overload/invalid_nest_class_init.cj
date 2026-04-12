// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A {
  static init() {
    class B {
      static init(){}
    }
  }
}

main() {
}

/* SCAN
error: unexpected class declaration in function body
  ==> invalid_nest_class_init.cj:11:5:
   | 
10 |   static init() {
   |          ~~~~ in function
11 |     class B {
   |     ^^^^^ unexpected class declaration
   | 

1 error generated, 1 error printed.
*/
