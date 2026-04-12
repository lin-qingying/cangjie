// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (not CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// (CJNATIVE) EXEC: %frontendCompiler %f

interface Bar {
    func bar<T>(a: T): T
    func test<T>(v: T) {
        return v
    }
}

open class Cls<X> {
    public open func test<T>(v: T) {
        return v
    }
}

abstract class Cls2<X> {
    public func foo<T>(v: T): T
    public open func test<T>(v: T) {
        return v
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: non-static generic function is not allowed in interface
 ==> generic_member_func_02.cj:4:5:
  |
4 |     func bar<T>(a: T) : T
  |     ^
  |

error: non-static generic function is not allowed in interface
 ==> generic_member_func_02.cj:5:5:
  |
5 |     func test<T>(v: T) {
  |     ^
  |

error: generic function cannot have 'open' modifier
  ==> generic_member_func_02.cj:11:5:
   |
11 |     public open func test<T>(v: T) {
   |     ^
   |

error: abstract generic function is not allowed in abstract class
  ==> generic_member_func_02.cj:17:5:
   |
17 |     public func foo<T>(v: T) :T
   |     ^
   |

error: generic function cannot have 'open' modifier
  ==> generic_member_func_02.cj:18:5:
   |
18 |     public open func test<T>(v: T) {
   |     ^
   |

5 errors generated, 5 errors printed.
 */
