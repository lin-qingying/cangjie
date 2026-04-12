// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let err = 1 + false
    let x = false
    x++ + 1
    x-- * 2

    var y = err
    y++
    y--

    let z = err
    z++
    z--

    0
}

/* SCAN
error: invalid binary operator '+' on type 'Int64' and 'Bool'
  ==> inc_dec_0.cj:10:17:
   | 
10 |     let err = 1 + false
   |                 ^ 
   | 
   # note: you may want to implement 'operator func +(right: Bool)' for type 'Int64'

error: mismatched types
  ==> inc_dec_0.cj:12:5:
   | 
12 |     x++ + 1
   |     ^ expected 'integer type', found 'Bool'
   | 
   # note: the base of increment or decrement expressions should be of integer type

error: mismatched types
  ==> inc_dec_0.cj:13:5:
   | 
13 |     x-- * 2
   |     ^ expected 'integer type', found 'Bool'
   | 
   # note: the base of increment or decrement expressions should be of integer type

error: cannot assign to immutable value
  ==> inc_dec_0.cj:12:5:
   | 
12 |     x++ + 1
   |     ^^^ 
   | 
note: variable 'x' is immutable
  ==> inc_dec_0.cj:11:9:
   | 
11 |     let x = false
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> inc_dec_0.cj:13:5:
   | 
13 |     x-- * 2
   |     ^^^ 
   | 
note: variable 'x' is immutable
  ==> inc_dec_0.cj:11:9:
   | 
11 |     let x = false
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> inc_dec_0.cj:20:5:
   | 
20 |     z++
   |     ^^^ 
   | 
note: variable 'z' is immutable
  ==> inc_dec_0.cj:19:9:
   | 
19 |     let z = err
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> inc_dec_0.cj:21:5:
   | 
21 |     z--
   |     ^^^ 
   | 
note: variable 'z' is immutable
  ==> inc_dec_0.cj:19:9:
   | 
19 |     let z = err
   |         ^ 
   | 

7 errors generated, 7 errors printed.
*/
