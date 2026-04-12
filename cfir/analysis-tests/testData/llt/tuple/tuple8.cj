// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let t = (0, 0)
    let x: String = t[0]
}

/* SCAN
error: mismatched types
  ==> tuple8.cj:11:21:
   | 
11 |     let x: String = t[0]
   |                     ^^^^ expected 'Struct-String', found 'Int64'
   | 

1 error generated, 1 error printed.
*/
