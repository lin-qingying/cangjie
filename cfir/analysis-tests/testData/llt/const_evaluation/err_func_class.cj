// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {}

extend A {
    func f() {
        0
    }
}

const func f() {
    let a = A()
    a.f()
}

main() {
    f()
}

/* SCAN
error: expected 'const' expression
  ==> err_func_class.cj:18:13:
   | 
18 |     let a = A()
   |             ^ 
   | 

error: expected 'const' expression
  ==> err_func_class.cj:19:5:
   | 
19 |     a.f()
   |     ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
