// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

const x = unsafe { 0 }

main() {
    x
}

/* SCAN
error: expected 'const' expression guaranteed to be evaluated at compile time
 ==> err_unsafe.cj:9:11:
  | 
9 | const x = unsafe { 0 }
  |           ^^^^^^^^^^^^ 
  | 

1 error generated, 1 error printed.
*/
