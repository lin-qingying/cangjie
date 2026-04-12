// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

var a: Int64 = 1.0 is Float64

main() {
    return 1
}

/* SCAN
error: mismatched types
 ==> is5.cj:9:16:
  | 
9 | var a: Int64 = 1.0 is Float64
  |                ^^^^^^^^^^^^^^ expected 'Int64', found 'Bool'
  | 

1 error generated, 1 error printed.
*/
