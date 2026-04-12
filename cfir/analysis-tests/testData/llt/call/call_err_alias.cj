// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

type F = () -> Unit

main() {
    F()
}

/* SCAN
error: called object is not a function or constructor
  ==> call_err_alias.cj:12:5:
   | 
12 |     F()
   |     ^ 
   | 
   # note: cannot call a type alias for a function type

1 error generated, 1 error printed.
*/
