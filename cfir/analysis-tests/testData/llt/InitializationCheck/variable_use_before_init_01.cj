// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

let y = (x = 1)
var x = 2

main() {}

/* SCAN
error: variable 'x' is used before being defined
 ==> variable_use_before_init_01.cj:9:10:
  | 
9 | let y = (x = 1)
  |          ^ 
  | 

1 error generated, 1 error printed.
*/
