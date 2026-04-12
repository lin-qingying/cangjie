// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (not CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// (CJNATIVE) EXEC: %frontendCompiler %f

abstract class A {
    public open func a<T>(c: T) {}
}

open class B {
    public open func b<T>(c: T) {}
}

main() {
    return 0
}

/* SCAN
error: generic function cannot have 'open' modifier
 ==> generic_func_limitation_3.cj:4:5:
  |
4 |     public open func a<T>(c: T) {}
  |     ^
  |

error: generic function cannot have 'open' modifier
 ==> generic_func_limitation_3.cj:8:5:
  |
8 |     public open func b<T>(c: T) {}
  |     ^
  |

2 errors generated, 2 errors printed.
 */
