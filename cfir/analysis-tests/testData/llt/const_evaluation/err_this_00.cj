// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    const func x() {
        0
    }
    const init() {}
    func f() {
        const t = x
        t
    }
}

main() {}

/* SCAN
error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_this_00.cj:15:19:
   | 
15 |         const t = x
   |                   ^ 
   | 

1 error generated, 1 error printed.
*/
