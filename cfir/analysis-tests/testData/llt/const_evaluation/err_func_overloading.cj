// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f() {}

const func f() {}

main() {}

/* SCAN
error: function 'f' has overload conflicts
  ==> err_func_overloading.cj:11:12:
   | 
11 | const func f() {}
   |            ^ 
   | 
note: conflict with the declaration
 ==> err_func_overloading.cj:9:6:
  | 
9 | func f() {}
  |      ^ 
  | 

1 error generated, 1 error printed.
*/
