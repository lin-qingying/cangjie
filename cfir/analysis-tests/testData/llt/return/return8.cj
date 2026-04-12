// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(x!: Bool = if (true) {
        func g(x!: Bool = return) {}
        true
    } else {
        false
    }, y!: Bool = x) {
    return y
}

main() {
    println(f())
}

/* SCAN
error: 'return' must be used inside a function body
  ==> return8.cj:10:27:
   | 
10 |         func g(x!: Bool = return) {}
   |                           ^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
