// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func mango() {
    "🥭"
}

let kiwi = "🥝"
let lemon = "🍋"

main() {
    const x = mango() + kiwi
    const y = mango() + lemon
    const z = kiwi * 3
}

/* SCAN
error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_desugar_string.cj:17:15:
   | 
17 |     const x = mango() + kiwi
   |               ^^^^^ 
   | 

error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_desugar_string.cj:17:25:
   | 
17 |     const x = mango() + kiwi
   |                         ^^^^ 
   | 

error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_desugar_string.cj:18:15:
   | 
18 |     const y = mango() + lemon
   |               ^^^^^ 
   | 

error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_desugar_string.cj:18:25:
   | 
18 |     const y = mango() + lemon
   |                         ^^^^^ 
   | 

error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_desugar_string.cj:19:15:
   | 
19 |     const z = kiwi * 3
   |               ^^^^ 
   | 

5 errors generated, 5 errors printed.
*/
