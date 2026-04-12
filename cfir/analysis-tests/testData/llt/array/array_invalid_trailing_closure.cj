// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE)EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    let arr1 = Array<Int64>() {i => i}

    let arr2 = Array<Int64>(1) {i}

    let arr3 = Array<Int64>(1, 2) {i => i}
    return 0
}

/* SCAN
error: missing argument for parameter list '(Int64, (Int64) -> Generics-T)' in call
  ==> array_invalid_trailing_closure.cj:10:28:
   | 
10 |     let arr1 = Array<Int64>() {i => i}
   |                            ^^^^^^^^^^^ expected 2 arguments, found 1
   | 
note: found candidate
  ==> (package std.core)array.cj:49:12:

error: undeclared identifier 'i'
  ==> array_invalid_trailing_closure.cj:12:33:
   | 
12 |     let arr2 = Array<Int64>(1) {i}
   |                                 ^ 
   | 

error: extra argument given for parameter list '(Int64, (Int64) -> Generics-T)' in call
  ==> array_invalid_trailing_closure.cj:14:28:
   | 
14 |     let arr3 = Array<Int64>(1, 2) {i => i}
   |                            ^^^^^^^^^^^^^^^ expected 2 arguments, found 3
   | 
note: found candidate
  ==> (package std.core)array.cj:49:12:

3 errors generated, 3 errors printed.
*/
