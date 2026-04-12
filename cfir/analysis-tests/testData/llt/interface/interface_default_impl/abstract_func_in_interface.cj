// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (not CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// (CJNATIVE) EXEC: %frontendCompiler %f
interface I {
    static func f1<T>(a: T): Unit {} // ok
    static func f2<T>(a: T): Unit // ok
    func f3<T>(a: T): Unit {} // error
    func f4<T>(a: T): Unit // error
}

main() {
    return 0
}

/* SCAN
error: non-static generic function is not allowed in interface
 ==> abstract_func_in_interface.cj:6:5:
  |
6 |     func f3<T>(a: T): Unit {} // error
  |     ^
  |

error: non-static generic function is not allowed in interface
 ==> abstract_func_in_interface.cj:7:5:
  |
7 |     func f4<T>(a: T): Unit // error
  |     ^
  |

2 errors generated, 2 errors printed.
 */
