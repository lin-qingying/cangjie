// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S {
    mut func f() {}
}

main() {
    S().f()
}

/* SCAN
error: cannot use mutable function on immutable value
  ==> mut_err_ret_struct.cj:14:5:
   | 
14 |     S().f()
   |     ^^^ ~ is a mutable function
   |     |
   |     is immutable
   | 

1 error generated, 1 error printed.
*/
