// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I {}

extend Int64 <: I {}

main() {
    var a = VArray<Int64, $3>(repeat: 0)
    var x: Int32 = 10
    // error
    a[3] == 0
    a[3] = 1
    a[-1] == 0
    a[-1] = 1

    let x1: Int64 = a[0] // ok
    let x2: I = a[0] // ok
    let x3: Int32 = a[0] // error
    a[0] = x1 // ok
    a[0] = x2 // error
    a[0] = x3 // error

    a[1, 2] // error
    a["1"] // error

    a[1 + 2 * 2] // error
    0
}

/* SCAN
error: mismatched types
  ==> varray_index01.cj:24:21:
   | 
24 |     let x3: Int32 = a[0] // error
   |                     ^^^^ expected 'Int32', found 'Int64'
   | 

error: mismatched types
  ==> varray_index01.cj:26:12:
   | 
26 |     a[0] = x2 // error
   |            ^^ expected 'Int64', found 'Interface-I'
   | 

error: mismatched types
  ==> varray_index01.cj:27:12:
   | 
27 |     a[0] = x3 // error
   |            ^^ expected 'Int64', found 'Int32'
   | 

error: 'VArray' accepts exactly one subscript index with type of 'Int64'
  ==> varray_index01.cj:29:5:
   | 
29 |     a[1, 2] // error
   |     ^^^^^^^ 
   | 

error: mismatched types
  ==> varray_index01.cj:30:7:
   | 
30 |     a["1"] // error
   |       ^^^ expected 'Int64', found 'Struct-String'
   | 

error: VArray index must be in bounds
  ==> varray_index01.cj:17:5:
   | 
17 |     a[3] == 0
   |     ^ ~ 'VArray' index 3 is past the end of 'VArray' (which contains 3 elements)
   | 

error: VArray index must be in bounds
  ==> varray_index01.cj:18:5:
   | 
18 |     a[3] = 1
   |     ^ ~ 'VArray' index 3 is past the end of 'VArray' (which contains 3 elements)
   | 

error: VArray index must be in bounds
  ==> varray_index01.cj:19:5:
   | 
19 |     a[-1] == 0
   |     ^ ~~ 'VArray' index can not be negative
   | 

error: VArray index must be in bounds
  ==> varray_index01.cj:20:5:
   | 
20 |     a[-1] = 1
   |     ^ ~~ 'VArray' index can not be negative
   | 

9 errors generated, 9 errors printed.
*/
