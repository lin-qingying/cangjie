// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func test(a: (Int64) -> Int32) {}

main() {
    test({i, j =>})
    test({i: Int64, j: Rune =>})
}

/* SCAN
error: mismatched number of parameters
  ==> lambda16.cj:12:11:
   | 
12 |     test({i, j =>})
   |           ^^^^ expected '1', found '2'
   | 

error: mismatched types
  ==> lambda16.cj:12:16:
   | 
12 |     test({i, j =>})
   |                ^^^ expected 'Int32', found 'Unit'
   | 

error: mismatched types
  ==> lambda16.cj:13:10:
   | 
13 |     test({i: Int64, j: Rune =>})
   |          ^^^^^^^^^^^^^^^^^^^^^^ expected '(Int64) -> Int32', found '(Int64, Rune) -> Unit'
   | 

3 errors generated, 3 errors printed.
*/
