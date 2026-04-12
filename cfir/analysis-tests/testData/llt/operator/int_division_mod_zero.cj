// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var _ = 9223372036854775808 % 0
    var _ = 9223372036854775808 / 0

    return 0
}

/* SCAN
error: invalid binary operator '%' on type 'UInt64' and 'Int64'
  ==> int_division_mod_zero.cj:10:33:
   | 
10 |     var _ = 9223372036854775808 % 0
   |                                 ^ 
   | 
   # note: you may want to implement 'operator func %(right: Int64)' for type 'UInt64'

error: invalid binary operator '/' on type 'UInt64' and 'Int64'
  ==> int_division_mod_zero.cj:11:33:
   | 
11 |     var _ = 9223372036854775808 / 0
   |                                 ^ 
   | 
   # note: you may want to implement 'operator func /(right: Int64)' for type 'UInt64'

2 errors generated, 2 errors printed.
*/
