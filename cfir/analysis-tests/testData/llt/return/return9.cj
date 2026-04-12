// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f(x!: Bool = if (true) {
        func g(x!: Bool = return false) {}
        return true
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
  ==> return9.cj:10:27:
   | 
10 |         func g(x!: Bool = return false) {}
   |                           ^^^^^^^^^^^^ 
   | 

error: 'return' must be used inside a function body
  ==> return9.cj:11:9:
   | 
11 |         return true
   |         ^^^^^^^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
