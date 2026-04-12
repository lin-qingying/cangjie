// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func ff1(a: Int64, arr: Array<Int64>) {}

func ff1(arr: Array<Int64>) {}

var ad = ff1(1, 2, 3)

main() {}

/* SCAN
error: ambiguous match for function call 'ff1'
  ==> variadic_err_ambiguous_01.cj:13:10:
   | 
13 | var ad = ff1(1, 2, 3)
   |          ^ 
   | 
note: found candidate
  ==> variadic_err_ambiguous_01.cj:11:6:
   | 
11 | func ff1(arr: Array<Int64>) {}
   |      ^ 
   | 
note: found candidate
 ==> variadic_err_ambiguous_01.cj:9:6:
  | 
9 | func ff1(a: Int64, arr: Array<Int64>) {}
  |      ^ 
  | 

1 error generated, 1 error printed.
*/
