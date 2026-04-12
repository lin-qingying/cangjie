// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) ERRCHECK: %compiler %cmp_opt %f -o %output

func run(): Int64 {
    var _: Int64 = 2 ** 62 + -2 ** 62 - 1
    var _: Int64 = 2 ** 63

    var _ = UInt64(2 ** 63 + 2 ** 63 - 1)
    var _ = UInt64(2 ** 64)
    return 0
}

main(): Int64 {
    return run();
}

/* SCAN
error: arithmetic operation '+' overflow
  ==> pow_overflow.cj:10:20:
   | 
10 |     var _: Int64 = 2 ** 62 + -2 ** 62 - 1
   |                    ^^^^^^^^^^^^^^^^^^ operation 'Int64(4611686018427387904) + Int64(4611686018427387904)' would overflow
   | 
   # note: range of Int64 is -9223372036854775808 ~ 9223372036854775807

error: arithmetic operation '**' overflow
  ==> pow_overflow.cj:11:20:
   | 
11 |     var _: Int64 = 2 ** 63
   |                    ^^^^^^^ operation 'Int64(2) ** UInt64(63)' would overflow
   | 
   # note: range of Int64 is -9223372036854775808 ~ 9223372036854775807

error: arithmetic operation '**' overflow
  ==> pow_overflow.cj:13:20:
   | 
13 |     var _ = UInt64(2 ** 63 + 2 ** 63 - 1)
   |                    ^^^^^^^ operation 'Int64(2) ** UInt64(63)' would overflow
   | 
   # note: range of Int64 is -9223372036854775808 ~ 9223372036854775807

error: arithmetic operation '**' overflow
  ==> pow_overflow.cj:13:30:
   | 
13 |     var _ = UInt64(2 ** 63 + 2 ** 63 - 1)
   |                              ^^^^^^^ operation 'Int64(2) ** UInt64(63)' would overflow
   | 
   # note: range of Int64 is -9223372036854775808 ~ 9223372036854775807

error: arithmetic operation '**' overflow
  ==> pow_overflow.cj:14:20:
   | 
14 |     var _ = UInt64(2 ** 64)
   |                    ^^^^^^^ operation 'Int64(2) ** UInt64(64)' would overflow
   | 
   # note: range of Int64 is -9223372036854775808 ~ 9223372036854775807

5 errors generated, 5 errors printed.
*/
