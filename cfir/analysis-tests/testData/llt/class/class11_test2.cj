// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface A {
    func i() {}
    static func test(input!: Unit = this.i()) {
        this.i()
    }
}

main(): Int64 {
    return 1
}

/* SCAN
error: optional parameter cannot be used in abstract function
  ==> class11_test2.cj:11:22:
   | 
11 |     static func test(input!: Unit = this.i()) {
   |                      ^^^^^^^^^^^^^^^^^^^^^^^ 
   | 

error: non-static variable 'this' cannot be referenced from a static context
  ==> class11_test2.cj:11:37:
   | 
11 |     static func test(input!: Unit = this.i()) {
   |                                     ^ 
   | 

error: non-static variable 'this' cannot be referenced from a static context
  ==> class11_test2.cj:12:9:
   | 
12 |         this.i()
   |         ^ 
   | 

3 errors generated, 3 errors printed.
*/
