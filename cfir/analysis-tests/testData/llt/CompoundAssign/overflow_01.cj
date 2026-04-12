// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main(): Int64 {
    var a: Int8 = 1
    var b: Int8 = 2
    var c: Int8 = 4
    var d: Int8 = 9
    a <<= 128
    a >>= 128
    a <<= -1
    a >>= -1
    a += 128
    b -= 128
    c *= 128
    d /= 128
    return 1
}

/* SCAN
error: the number '128' exceeds the value range of type 'Int8'
  ==> overflow_01.cj:14:11:
   | 
14 |     a <<= 128
   |           ^^^ 
   | 

error: the number '128' exceeds the value range of type 'Int8'
  ==> overflow_01.cj:15:11:
   | 
15 |     a >>= 128
   |           ^^^ 
   | 

error: shift count is negative
  ==> overflow_01.cj:16:11:
   | 
16 |     a <<= -1
   |           ^ 
   | 

error: shift count is negative
  ==> overflow_01.cj:17:11:
   | 
17 |     a >>= -1
   |           ^ 
   | 

error: the number '128' exceeds the value range of type 'Int8'
  ==> overflow_01.cj:18:10:
   | 
18 |     a += 128
   |          ^^^ 
   | 

error: the number '128' exceeds the value range of type 'Int8'
  ==> overflow_01.cj:19:10:
   | 
19 |     b -= 128
   |          ^^^ 
   | 

error: the number '128' exceeds the value range of type 'Int8'
  ==> overflow_01.cj:20:10:
   | 
20 |     c *= 128
   |          ^^^ 
   | 

error: the number '128' exceeds the value range of type 'Int8'
  ==> overflow_01.cj:21:10:
   | 
21 |     d /= 128
   |          ^^^ 
   | 

8 errors generated, 8 errors printed.
*/
