// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (not CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// (CJNATIVE) EXEC: %frontendCompiler %f

interface A {
    func f<T>(a: T): T
}

main() {}

/* SCAN
error: non-static generic function is not allowed in interface
 ==> generic_func_limitation_1.cj:4:5:
  |
4 |     func f<T>(a: T):T
  |     ^
  |

1 error generated, 1 error printed.
 */
