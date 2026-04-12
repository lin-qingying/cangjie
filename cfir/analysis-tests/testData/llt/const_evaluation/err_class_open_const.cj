// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    // const member function cannot be open
    open const func f() { 0 }
    const init() {}
}

main() {
    let a = A()
    a.f()
}

/* SCAN
error: 'const' and 'open' modifiers conflict on function declaration
  ==> err_class_open_const.cj:11:10:
   | 
11 |     open const func f() { 0 }
   |     ~~~~ ^^^^^ ~~~~ on function declaration
   |     |    |
   |     |    unexpected modifier
   |     |
   |     because it is conflicted with this
   | 

1 error generated, 1 error printed.
*/
