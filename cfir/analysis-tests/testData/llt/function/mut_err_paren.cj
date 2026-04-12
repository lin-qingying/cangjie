// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S {
    mut func f() {}
}

class A {
    let s = S()
}

main() {
    let a = A()
    ((((a)))).s.f()
}

/* SCAN
error: cannot use mutable function on immutable value
  ==> mut_err_paren.cj:19:5:
   | 
19 |     ((((a)))).s.f()
   |     ^^^^^^^^^^^ ~ is a mutable function
   |     |
   |     is immutable
   | 
note: 's' is a variable declared with 'let'
  ==> mut_err_paren.cj:14:9:
   | 
14 |     let s = S()
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
