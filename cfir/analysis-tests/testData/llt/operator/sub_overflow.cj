// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func run(): Int64 {
    var _: Int8 = -127 - 1
    var _: Int8 = -128 - 1
    var _: Int8 = 127 - (-1)
    var _: Int16 = -32767 - 1
    var _: Int16 = -32768 - 1
    var _: Int16 = 32767 - (-1)
    var _: Int32 = -2147483647 - 1
    var _: Int32 = -2147483648 - 1
    var _: Int32 = 2147483647 - (-1)
    var _: Int64 = -9223372036854775807 - 1
    var _: Int64 = -9223372036854775808 - 1
    var _: Int64 = 9223372036854775807 - (-1)
    var _: UInt8 = 0 - 1
    var _: UInt16 = 0 - 1
    var _: UInt32 = 0 - 1
    var _: UInt64 = 0 - 1

    var _: Int8 = -128 / -1
    var _: Int16 = -32768 / -1
    var _: Int32 = -2147483648 / -1
    var _: Int64 = -9223372036854775808 / -1

    var _: Int8 = -(-127 - 1)
    var _: Int16 = -(-32767 - 1)
    var _: Int32 = -(-2147483647 - 1)
    var _: Int64 = -(-9223372036854775807 - 1)
    var _: UInt8 = -(1 + 2)
    var _: UInt16 = -(1 + 2)
    var _: UInt32 = -(1 + 2)
    var _: UInt64 = -(1 + 2)

    return 0
}

main(): Int64 {
    return run();
}

/* SCAN
error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:11:19:
   | 
11 |     var _: Int8 = -128 - 1
   |                   ^^^^^^^^ operation 'Int8(-128) - Int8(1)' would overflow
   | 
   # note: range of Int8 is -128 ~ 127

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:12:19:
   | 
12 |     var _: Int8 = 127 - (-1)
   |                   ^^^^^^^^^^ operation 'Int8(127) - Int8(-1)' would overflow
   | 
   # note: range of Int8 is -128 ~ 127

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:14:20:
   | 
14 |     var _: Int16 = -32768 - 1
   |                    ^^^^^^^^^^ operation 'Int16(-32768) - Int16(1)' would overflow
   | 
   # note: range of Int16 is -32768 ~ 32767

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:15:20:
   | 
15 |     var _: Int16 = 32767 - (-1)
   |                    ^^^^^^^^^^^^ operation 'Int16(32767) - Int16(-1)' would overflow
   | 
   # note: range of Int16 is -32768 ~ 32767

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:17:20:
   | 
17 |     var _: Int32 = -2147483648 - 1
   |                    ^^^^^^^^^^^^^^^ operation 'Int32(-2147483648) - Int32(1)' would overflow
   | 
   # note: range of Int32 is -2147483648 ~ 2147483647

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:18:20:
   | 
18 |     var _: Int32 = 2147483647 - (-1)
   |                    ^^^^^^^^^^^^^^^^^ operation 'Int32(2147483647) - Int32(-1)' would overflow
   | 
   # note: range of Int32 is -2147483648 ~ 2147483647

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:20:20:
   | 
20 |     var _: Int64 = -9223372036854775808 - 1
   |                    ^^^^^^^^^^^^^^^^^^^^^^^^ operation 'Int64(-9223372036854775808) - Int64(1)' would overflow
   | 
   # note: range of Int64 is -9223372036854775808 ~ 9223372036854775807

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:21:20:
   | 
21 |     var _: Int64 = 9223372036854775807 - (-1)
   |                    ^^^^^^^^^^^^^^^^^^^^^^^^^^ operation 'Int64(9223372036854775807) - Int64(-1)' would overflow
   | 
   # note: range of Int64 is -9223372036854775808 ~ 9223372036854775807

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:22:20:
   | 
22 |     var _: UInt8 = 0 - 1
   |                    ^^^^^ operation 'UInt8(0) - UInt8(1)' would overflow
   | 
   # note: range of UInt8 is 0 ~ 255

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:23:21:
   | 
23 |     var _: UInt16 = 0 - 1
   |                     ^^^^^ operation 'UInt16(0) - UInt16(1)' would overflow
   | 
   # note: range of UInt16 is 0 ~ 65535

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:24:21:
   | 
24 |     var _: UInt32 = 0 - 1
   |                     ^^^^^ operation 'UInt32(0) - UInt32(1)' would overflow
   | 
   # note: range of UInt32 is 0 ~ 4294967295

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:25:21:
   | 
25 |     var _: UInt64 = 0 - 1
   |                     ^^^^^ operation 'UInt64(0) - UInt64(1)' would overflow
   | 
   # note: range of UInt64 is 0 ~ 18446744073709551615

error: arithmetic operation '/' overflow
  ==> sub_overflow.cj:27:19:
   | 
27 |     var _: Int8 = -128 / -1
   |                   ^^^^^^^^^ operation 'Int8(-128) / Int8(-1)' would overflow
   | 
   # note: range of Int8 is -128 ~ 127

error: arithmetic operation '/' overflow
  ==> sub_overflow.cj:28:20:
   | 
28 |     var _: Int16 = -32768 / -1
   |                    ^^^^^^^^^^^ operation 'Int16(-32768) / Int16(-1)' would overflow
   | 
   # note: range of Int16 is -32768 ~ 32767

error: arithmetic operation '/' overflow
  ==> sub_overflow.cj:29:20:
   | 
29 |     var _: Int32 = -2147483648 / -1
   |                    ^^^^^^^^^^^^^^^^ operation 'Int32(-2147483648) / Int32(-1)' would overflow
   | 
   # note: range of Int32 is -2147483648 ~ 2147483647

error: arithmetic operation '/' overflow
  ==> sub_overflow.cj:30:20:
   | 
30 |     var _: Int64 = -9223372036854775808 / -1
   |                    ^^^^^^^^^^^^^^^^^^^^^^^^^ operation 'Int64(-9223372036854775808) / Int64(-1)' would overflow
   | 
   # note: range of Int64 is -9223372036854775808 ~ 9223372036854775807

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:32:19:
   | 
32 |     var _: Int8 = -(-127 - 1)
   |                   ^^^^^^^^^^^ operation '-Int8(-128)' would overflow
   | 
   # note: range of Int8 is -128 ~ 127

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:33:20:
   | 
33 |     var _: Int16 = -(-32767 - 1)
   |                    ^^^^^^^^^^^^^ operation '-Int16(-32768)' would overflow
   | 
   # note: range of Int16 is -32768 ~ 32767

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:34:20:
   | 
34 |     var _: Int32 = -(-2147483647 - 1)
   |                    ^^^^^^^^^^^^^^^^^^ operation '-Int32(-2147483648)' would overflow
   | 
   # note: range of Int32 is -2147483648 ~ 2147483647

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:35:20:
   | 
35 |     var _: Int64 = -(-9223372036854775807 - 1)
   |                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^ operation '-Int64(-9223372036854775808)' would overflow
   | 
   # note: range of Int64 is -9223372036854775808 ~ 9223372036854775807

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:36:20:
   | 
36 |     var _: UInt8 = -(1 + 2)
   |                    ^^^^^^^^ operation '-UInt8(3)' would overflow
   | 
   # note: range of UInt8 is 0 ~ 255

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:37:21:
   | 
37 |     var _: UInt16 = -(1 + 2)
   |                     ^^^^^^^^ operation '-UInt16(3)' would overflow
   | 
   # note: range of UInt16 is 0 ~ 65535

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:38:21:
   | 
38 |     var _: UInt32 = -(1 + 2)
   |                     ^^^^^^^^ operation '-UInt32(3)' would overflow
   | 
   # note: range of UInt32 is 0 ~ 4294967295

error: arithmetic operation '-' overflow
  ==> sub_overflow.cj:39:21:
   | 
39 |     var _: UInt64 = -(1 + 2)
   |                     ^^^^^^^^ operation '-UInt64(3)' would overflow
   | 
   # note: range of UInt64 is 0 ~ 18446744073709551615

warning: unused function:'main'
  ==> sub_overflow.cj:44:1:
   | 
44 |   main(): Int64 {
   |  _^
45 | |     return run();
46 | | }
   | |_^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

24 errors generated, 24 errors printed.
1 warning generated, 1 warning printed.
*/
