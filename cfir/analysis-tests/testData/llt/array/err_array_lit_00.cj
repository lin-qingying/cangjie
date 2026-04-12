// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface I {}

main() {
    let _: I = ([1])
}

/* SCAN
error: mismatched types
  ==> err_array_lit_00.cj:12:17:
   | 
12 |     let _: I = ([1])
   |                 ^^^ expected 'Interface-I', found 'Struct-Array<Int64>'
   | 

1 error generated, 1 error printed.
*/
