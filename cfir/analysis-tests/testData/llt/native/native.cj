// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %cmp_opt %f 2>&1 | compare %f

//brief:IntNative赋值为常量计算且溢出，编译报错
main(): Int64 {
    var _: IntNative = -9223372036854775808 - 1
    var _: IntNative = 9223372036854775807 + 1
    return 0
}

/* SCAN
error: arithmetic operation '-' overflow
  ==> native.cj:11:24:
   | 
11 |     var _: IntNative = -9223372036854775808 - 1
   |                        ^^^^^^^^^^^^^^^^^^^^^^^^ operation 'IntNative(-9223372036854775808) - IntNative(1)' would overflow
   | 
   # note: range of IntNative is -9223372036854775808 ~ 9223372036854775807

error: arithmetic operation '+' overflow
  ==> native.cj:12:24:
   | 
12 |     var _: IntNative = 9223372036854775807 + 1
   |                        ^^^^^^^^^^^^^^^^^^^^^^^ operation 'IntNative(9223372036854775807) + IntNative(1)' would overflow
   | 
   # note: range of IntNative is -9223372036854775808 ~ 9223372036854775807

2 errors generated, 2 errors printed.
*/
