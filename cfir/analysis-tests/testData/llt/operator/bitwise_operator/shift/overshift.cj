// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var _: Int8 = 10 << 8
    var _: Int8 = 10 << 9
    var _: Int8 = 10 >> 9
    var _: Int16 = 10 << 16
    var _: Int16 = 10 << 17
    var _: Int32 = 10 >> 33
    var _: Int64 = 10 >> 65
    var _: UInt8 = 10 >> -1
    var _: UInt8 = 10 >> 10
    var _: UInt16 = 10 >> 18
    var _: UInt32 = 10 >> 33
    var _: UInt64 = 10 << 128
    var rfa: UInt8 = 10
    rfa << 9
    127 << -1
    var _ = 10 << 65
    return 0;
}

/* SCAN
warning: unused function:'main'
  ==> overshift.cj:9:1:
   | 
9  |   main(): Int64 {
   |  _^
10 | |     var _: Int8 = 10 << 8
   | | ...
27 | | }
   | |_^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

error: shift operation overflow
  ==> overshift.cj:10:19:
   | 
10 |     var _: Int8 = 10 << 8
   |                   ^^^^^^^ attempt to shift 8 bits on Int8 type
   | 
   # note: the type of left operand is Int8, the most bits that expected to shift are 7

error: shift operation overflow
  ==> overshift.cj:11:19:
   | 
11 |     var _: Int8 = 10 << 9
   |                   ^^^^^^^ attempt to shift 9 bits on Int8 type
   | 
   # note: the type of left operand is Int8, the most bits that expected to shift are 7

error: shift operation overflow
  ==> overshift.cj:12:19:
   | 
12 |     var _: Int8 = 10 >> 9
   |                   ^^^^^^^ attempt to shift 9 bits on Int8 type
   | 
   # note: the type of left operand is Int8, the most bits that expected to shift are 7

error: shift operation overflow
  ==> overshift.cj:13:20:
   | 
13 |     var _: Int16 = 10 << 16
   |                    ^^^^^^^^ attempt to shift 16 bits on Int16 type
   | 
   # note: the type of left operand is Int16, the most bits that expected to shift are 15

error: shift operation overflow
  ==> overshift.cj:14:20:
   | 
14 |     var _: Int16 = 10 << 17
   |                    ^^^^^^^^ attempt to shift 17 bits on Int16 type
   | 
   # note: the type of left operand is Int16, the most bits that expected to shift are 15

error: shift operation overflow
  ==> overshift.cj:15:20:
   | 
15 |     var _: Int32 = 10 >> 33
   |                    ^^^^^^^^ attempt to shift 33 bits on Int32 type
   | 
   # note: the type of left operand is Int32, the most bits that expected to shift are 31

error: shift operation overflow
  ==> overshift.cj:16:20:
   | 
16 |     var _: Int64 = 10 >> 65
   |                    ^^^^^^^^ attempt to shift 65 bits on Int64 type
   | 
   # note: the type of left operand is Int64, the most bits that expected to shift are 63

error: shift operation overflow
  ==> overshift.cj:17:20:
   | 
17 |     var _: UInt8 = 10 >> -1
   |                    ^^^^^^^^ attempt to shift -1 bits on UInt8 type
   | 
   # note: right operand can not be negative

error: shift operation overflow
  ==> overshift.cj:18:20:
   | 
18 |     var _: UInt8 = 10 >> 10
   |                    ^^^^^^^^ attempt to shift 10 bits on UInt8 type
   | 
   # note: the type of left operand is UInt8, the most bits that expected to shift are 7

error: shift operation overflow
  ==> overshift.cj:19:21:
   | 
19 |     var _: UInt16 = 10 >> 18
   |                     ^^^^^^^^ attempt to shift 18 bits on UInt16 type
   | 
   # note: the type of left operand is UInt16, the most bits that expected to shift are 15

error: shift operation overflow
  ==> overshift.cj:20:21:
   | 
20 |     var _: UInt32 = 10 >> 33
   |                     ^^^^^^^^ attempt to shift 33 bits on UInt32 type
   | 
   # note: the type of left operand is UInt32, the most bits that expected to shift are 31

error: shift operation overflow
  ==> overshift.cj:21:21:
   | 
21 |     var _: UInt64 = 10 << 128
   |                     ^^^^^^^^^ attempt to shift 128 bits on UInt64 type
   | 
   # note: the type of left operand is UInt64, the most bits that expected to shift are 63

error: shift operation overflow
  ==> overshift.cj:23:5:
   | 
23 |     rfa << 9
   |     ^^^^^^^^ attempt to shift 9 bits on UInt8 type
   | 
   # note: the type of left operand is UInt8, the most bits that expected to shift are 7

error: shift operation overflow
  ==> overshift.cj:24:5:
   | 
24 |     127 << -1
   |     ^^^^^^^^^ attempt to shift -1 bits on Int64 type
   | 
   # note: right operand can not be negative

error: shift operation overflow
  ==> overshift.cj:25:13:
   | 
25 |     var _ = 10 << 65
   |             ^^^^^^^^ attempt to shift 65 bits on Int64 type
   | 
   # note: the type of left operand is Int64, the most bits that expected to shift are 63

15 errors generated, 15 errors printed.
1 warning generated, 1 warning printed.
*/
