// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func run(): Int64 {
    var ao = 18446744073709551616
    var auo = -9223372036854775809
    var bo: Int8 = 128
    var buo: Int8 = -129
    var ubo: UInt8 = 256
    var ubuo: UInt8 = -1

    var co: Int16 = 32768
    var cuo: Int16 = -32769
    var uco: UInt16 = 65536
    var ucuo: UInt16 = -1

    var eo: Int32 = 2147483648
    var euo: Int32 = -2147483649
    var ueo: UInt32 = 42949672956
    var ueuo: UInt32 = -1

    var fo: Int64 = 9223372036854775808
    var fuo: Int64 = -9223372036854775809
    var ufo: UInt64 = 18446744073709551616
    var ufuo: UInt64 = -1
    return 0
}

main(): Int64 {
    return run();
}

/* SCAN
error: the number '18446744073709551616' exceeds the value range of type 'Int64'
  ==> num_overflow.cj:10:14:
   | 
10 |     var ao = 18446744073709551616
   |              ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '-9223372036854775809' exceeds the value range of type 'Int64'
  ==> num_overflow.cj:11:15:
   | 
11 |     var auo = -9223372036854775809
   |               ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '128' exceeds the value range of type 'Int8'
  ==> num_overflow.cj:12:20:
   | 
12 |     var bo: Int8 = 128
   |                    ^^^ 
   | 

error: the number '-129' exceeds the value range of type 'Int8'
  ==> num_overflow.cj:13:21:
   | 
13 |     var buo: Int8 = -129
   |                     ^^^^ 
   | 

error: the number '256' exceeds the value range of type 'UInt8'
  ==> num_overflow.cj:14:22:
   | 
14 |     var ubo: UInt8 = 256
   |                      ^^^ 
   | 

error: the number '-1' exceeds the value range of type 'UInt8'
  ==> num_overflow.cj:15:23:
   | 
15 |     var ubuo: UInt8 = -1
   |                       ^^ 
   | 

error: the number '32768' exceeds the value range of type 'Int16'
  ==> num_overflow.cj:17:21:
   | 
17 |     var co: Int16 = 32768
   |                     ^^^^^ 
   | 

error: the number '-32769' exceeds the value range of type 'Int16'
  ==> num_overflow.cj:18:22:
   | 
18 |     var cuo: Int16 = -32769
   |                      ^^^^^^ 
   | 

error: the number '65536' exceeds the value range of type 'UInt16'
  ==> num_overflow.cj:19:23:
   | 
19 |     var uco: UInt16 = 65536
   |                       ^^^^^ 
   | 

error: the number '-1' exceeds the value range of type 'UInt16'
  ==> num_overflow.cj:20:24:
   | 
20 |     var ucuo: UInt16 = -1
   |                        ^^ 
   | 

error: the number '2147483648' exceeds the value range of type 'Int32'
  ==> num_overflow.cj:22:21:
   | 
22 |     var eo: Int32 = 2147483648
   |                     ^^^^^^^^^^ 
   | 

error: the number '-2147483649' exceeds the value range of type 'Int32'
  ==> num_overflow.cj:23:22:
   | 
23 |     var euo: Int32 = -2147483649
   |                      ^^^^^^^^^^^ 
   | 

error: the number '42949672956' exceeds the value range of type 'UInt32'
  ==> num_overflow.cj:24:23:
   | 
24 |     var ueo: UInt32 = 42949672956
   |                       ^^^^^^^^^^^ 
   | 

error: the number '-1' exceeds the value range of type 'UInt32'
  ==> num_overflow.cj:25:24:
   | 
25 |     var ueuo: UInt32 = -1
   |                        ^^ 
   | 

error: the number '9223372036854775808' exceeds the value range of type 'Int64'
  ==> num_overflow.cj:27:21:
   | 
27 |     var fo: Int64 = 9223372036854775808
   |                     ^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '-9223372036854775809' exceeds the value range of type 'Int64'
  ==> num_overflow.cj:28:22:
   | 
28 |     var fuo: Int64 = -9223372036854775809
   |                      ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '18446744073709551616' exceeds the value range of type 'UInt64'
  ==> num_overflow.cj:29:23:
   | 
29 |     var ufo: UInt64 = 18446744073709551616
   |                       ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '-1' exceeds the value range of type 'UInt64'
  ==> num_overflow.cj:30:24:
   | 
30 |     var ufuo: UInt64 = -1
   |                        ^^ 
   | 

18 errors generated, 18 errors printed.
*/
