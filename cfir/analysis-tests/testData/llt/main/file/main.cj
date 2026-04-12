// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main(): Int64 {}

/* SCAN
error: mismatched types
 ==> main.cj:9:15:
  | 
9 | main(): Int64 {}
  |         ~~~~~ ^^ expected 'Int64', found 'Unit'
  |         |
  |         expected 'Int64' because of return type
  | 

1 error generated, 1 error printed.
*/
