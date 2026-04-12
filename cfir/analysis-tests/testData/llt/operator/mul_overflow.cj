// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func run(): Int64 {
    var _: Int8 = 2 * 64
    var _: Int8 = -2 * -64
    var _: Int8 = -2 * 65
    var _: Int16 = 64 * 512
    var _: Int32 = 1024 * 1024 * 2048
    var _: Int32 = -17 * 65 * -1024 * -2048
    var _: Int64 = 2305843009213693952 * 4
    var _: Int64 = -3074457345618258603 * 3

    var _: UInt8 = 4 * 64
    var _: UInt16 = 64 * 1024
    var _: UInt32 = 1024 * 2048 * 2048
    var _: UInt64 = 4 * 2147483648 * 2147483648
    var _: UInt64 = 18446744073709551615 * 2
    var _: UInt64 = 18446744073709551614 * -18446744073709551615
    var _: UInt64 = -18446744073709551615 * -18446744073709551615

    return 0
}

main(): Int64 {
    return run();
}

/* SCAN
error: the number '-18446744073709551615' exceeds the value range of type 'Int64'
  ==> mul_overflow.cj:24:44:
   | 
24 |     var _: UInt64 = 18446744073709551614 * -18446744073709551615
   |                                            ^^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '-18446744073709551615' exceeds the value range of type 'Int64'
  ==> mul_overflow.cj:25:21:
   | 
25 |     var _: UInt64 = -18446744073709551615 * -18446744073709551615
   |                     ^^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the number '-18446744073709551615' exceeds the value range of type 'Int64'
  ==> mul_overflow.cj:25:45:
   | 
25 |     var _: UInt64 = -18446744073709551615 * -18446744073709551615
   |                                             ^^^^^^^^^^^^^^^^^^^^^ 
   | 

3 errors generated, 3 errors printed.
*/
