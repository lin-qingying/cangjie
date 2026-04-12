// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f() {
    const y = match (0) {
        case _ where return => 0
        case _ => 1
    }
}

main() {}

/* SCAN
error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_return.cj:11:22:
   | 
11 |         case _ where return => 0
   |                      ^^^^^^ 
   | 
   # note: cannot jump to non-constant context from a constant context

1 error generated, 1 error printed.
*/
