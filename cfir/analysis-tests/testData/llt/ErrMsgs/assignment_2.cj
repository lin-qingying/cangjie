// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

// Should only report type mismatch at line 10

class A {
    operator func -() {
        1
    }
}

main() {
    let c = 1
    let a: Rune = -A()
    let b = - r'c'
}

/* SCAN
error: mismatched types
  ==> assignment_2.cj:19:19:
   | 
19 |     let a: Rune = -A()
   |                   ^^^^ expected 'Rune', found 'Int64'
   | 

error: invalid unary operator '-' on type 'Rune'
  ==> assignment_2.cj:20:13:
   | 
20 |     let b = - r'c'
   |             ^^^^^^ 
   | 
   # note: you may want to implement 'operator func -()' for type 'Rune'

2 errors generated, 2 errors printed.
*/
