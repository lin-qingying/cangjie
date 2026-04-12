// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

const x = t

main() {
    x
}

/* SCAN
error: undeclared identifier 't'
 ==> err_no_target.cj:9:11:
  | 
9 | const x = t
  |           ^ 
  | 

1 error generated, 1 error printed.
*/
