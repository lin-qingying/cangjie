// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    println(127i8 - -128i8)
}

/* SCAN
error: arithmetic operation '-' overflow
  ==> sub_overflow_01.cj:10:13:
   | 
10 |     println(127i8 - -128i8)
   |             ^^^^^^^^^^^^^^ operation 'Int8(127) - Int8(-128)' would overflow
   | 
   # note: range of Int8 is -128 ~ 127

1 error generated, 1 error printed.
*/
