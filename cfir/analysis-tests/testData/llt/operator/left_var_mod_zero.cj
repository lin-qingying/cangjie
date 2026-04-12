// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f 2>&1 | compare %f

main() {
    var a = 10
    var _ = a % 0
    var _ = a / 0
    return 0
}

/* SCAN
error: modulo by zero
  ==> left_var_mod_zero.cj:11:13:
   | 
11 |     var _ = a % 0
   |             ^^^^^ attempt to modulo by zero
   | 

error: divide by zero
  ==> left_var_mod_zero.cj:12:13:
   | 
12 |     var _ = a / 0
   |             ^^^^^ attempt to divide by zero
   | 

2 errors generated, 2 errors printed.
*/
