// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func test(a: (Int64) -> Int32) {}

main() {
    let _ = Array<Int32>(5, {=>})

    test({=>})
}

/* SCAN
error: mismatched types
  ==> lambda15.cj:12:29:
   | 
12 |     let _ = Array<Int32>(5, {=>})
   |                             ^^^^ expected '(Int64) -> Int32', found '() -> Unit'
   | 

error: mismatched types
  ==> lambda15.cj:14:10:
   | 
14 |     test({=>})
   |          ^^^^ expected '(Int64) -> Int32', found '() -> Unit'
   | 

2 errors generated, 2 errors printed.
*/
