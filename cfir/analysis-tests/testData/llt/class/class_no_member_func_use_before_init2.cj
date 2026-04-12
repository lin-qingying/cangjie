// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class A {
    public var a = f
    public var b: () -> Int64
    public var d = g
    public static var e = g
    public init(x: () -> Int64) {
        func g() {
            0
        }
        b = g
        a = x
    }
    public init() {
        b = g
        var h = f()
        a = f
        a = this.f
    }
    public func f(): Int64 {
        return 0
    }
    public static func g(): Int64 {
        return 1
    }
}

/* SCAN
error: 'f' is not allowed to be accessed before all member variables are initialized
  ==> class_no_member_func_use_before_init2.cj:10:20:
   | 
10 |     public var a = f
   |                    ^ 
   | 

1 error generated, 1 error printed.
*/
