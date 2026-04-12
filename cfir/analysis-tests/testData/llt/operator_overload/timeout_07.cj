// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
// TIMEOUT: 10

class A {
    operator func -() {
        A()
    }
}

class B {
    operator func !() {
        B()
    }
}

func f(_: (A) -> A) {}

func f(_: (B) -> B) {}

main() {
    f({a => !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!-a})
}

/* SCAN
error: invalid unary operator '!' on type 'Class-A'
  ==> timeout_07.cj:27:78:
   | 
27 |     f({a => !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!-a})
   |                                                                              ^^^ 
   | 
   # note: you may want to implement 'operator func !()' for type 'Class-A'

1 error generated, 1 error printed.
*/
