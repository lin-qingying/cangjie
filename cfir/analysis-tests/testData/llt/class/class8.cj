// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class A {
    public init() {}
    public init(a: Int64) {
        this()
        this.a = a
    }
    public func test() {
        var res = this(1)
    }
    public var a: Int64 = 1
    public var b = this()
}

/* SCAN
error: invalid calling 'this' outside the constructor
  ==> class8.cj:16:19:
   | 
16 |         var res = this(1)
   |                   ^ 
   | 

error: invalid calling 'this' outside the constructor
  ==> class8.cj:19:20:
   | 
19 |     public var b = this()
   |                    ^ 
   | 

2 errors generated, 2 errors printed.
*/
