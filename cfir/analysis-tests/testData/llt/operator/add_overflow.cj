// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func run(): Int64 {
    var _: Int8 = 127 + 1
    var _: Int16 = 32767 + 1
    var _: Int32 = 2147483647 + 1
    var _: Int64 = 9223372036854775807 + 1
    var _: UInt8 = 255 + 1
    var _: UInt16 = 65535 + 1
    var _: UInt32 = 4294967295 + 1
    var _: UInt64 = 18446744073709551615 + 1
    var _: UInt64 = (4294968200 << 31) + 9223373978179993602

    return 0
}

main(): Int64 {
    return run();
}

/* SCAN
error: arithmetic operation '+' overflow
  ==> add_overflow.cj:10:19:
   | 
10 |     var _: Int8 = 127 + 1
   |                   ^^^^^^^ operation 'Int8(127) + Int8(1)' would overflow
   | 
   # note: range of Int8 is -128 ~ 127

error: arithmetic operation '+' overflow
  ==> add_overflow.cj:11:20:
   | 
11 |     var _: Int16 = 32767 + 1
   |                    ^^^^^^^^^ operation 'Int16(32767) + Int16(1)' would overflow
   | 
   # note: range of Int16 is -32768 ~ 32767

error: arithmetic operation '+' overflow
  ==> add_overflow.cj:12:20:
   | 
12 |     var _: Int32 = 2147483647 + 1
   |                    ^^^^^^^^^^^^^^ operation 'Int32(2147483647) + Int32(1)' would overflow
   | 
   # note: range of Int32 is -2147483648 ~ 2147483647

error: arithmetic operation '+' overflow
  ==> add_overflow.cj:13:20:
   | 
13 |     var _: Int64 = 9223372036854775807 + 1
   |                    ^^^^^^^^^^^^^^^^^^^^^^^ operation 'Int64(9223372036854775807) + Int64(1)' would overflow
   | 
   # note: range of Int64 is -9223372036854775808 ~ 9223372036854775807

error: arithmetic operation '+' overflow
  ==> add_overflow.cj:14:20:
   | 
14 |     var _: UInt8 = 255 + 1
   |                    ^^^^^^^ operation 'UInt8(255) + UInt8(1)' would overflow
   | 
   # note: range of UInt8 is 0 ~ 255

error: arithmetic operation '+' overflow
  ==> add_overflow.cj:15:21:
   | 
15 |     var _: UInt16 = 65535 + 1
   |                     ^^^^^^^^^ operation 'UInt16(65535) + UInt16(1)' would overflow
   | 
   # note: range of UInt16 is 0 ~ 65535

error: arithmetic operation '+' overflow
  ==> add_overflow.cj:16:21:
   | 
16 |     var _: UInt32 = 4294967295 + 1
   |                     ^^^^^^^^^^^^^^ operation 'UInt32(4294967295) + UInt32(1)' would overflow
   | 
   # note: range of UInt32 is 0 ~ 4294967295

error: arithmetic operation '+' overflow
  ==> add_overflow.cj:17:21:
   | 
17 |     var _: UInt64 = 18446744073709551615 + 1
   |                     ^^^^^^^^^^^^^^^^^^^^^^^^ operation 'UInt64(18446744073709551615) + UInt64(1)' would overflow
   | 
   # note: range of UInt64 is 0 ~ 18446744073709551615

error: arithmetic operation '+' overflow
  ==> add_overflow.cj:18:21:
   | 
18 |     var _: UInt64 = (4294968200 << 31) + 9223373978179993602
   |                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ operation 'UInt64(9223373978179993600) + UInt64(9223373978179993602)' would overflow
   | 
   # note: range of UInt64 is 0 ~ 18446744073709551615

warning: unused function:'main'
  ==> add_overflow.cj:23:1:
   | 
23 |   main(): Int64 {
   |  _^
24 | |     return run();
25 | | }
   | |_^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

9 errors generated, 9 errors printed.
1 warning generated, 1 warning printed.
*/
