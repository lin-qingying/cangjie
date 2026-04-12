// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

type ran = Range<Int64

func test(a: ran) {
    let arr = [1]
    arr[a]
    0
}

/* SCAN
error: unclosed delimiter: '<'
 ==> fuzz_bug.cj:9:23:
  | 
9 | type ran = Range<Int64
  |                 ~     ^ expected '>' here
  |                 |
  |                 to match this opening '<'
  | 

1 error generated, 1 error printed.
*/
