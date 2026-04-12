// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

struct A {
    public var a: Int64 = 1
    public static var c: Int64 = 2

    public var b: () -> Int64 = {=> a}
    public func f() {
        var d: Int64 = 1
        a
        b()
        var c: () -> Int64 = {=> d}
        func ff() {
            var b: Int64 = a
        }
        return a
    }
    public static func f1() {
        c
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: 'a' is not allowed to be accessed before all member variables are initialized
  ==> function_cannot_capture_var1.cj:13:37:
   | 
13 |     public var b: () -> Int64 = {=> a}
   |                                     ^ 
   | 

error: lambda capturing mutable variables needs to be called directly
  ==> function_cannot_capture_var1.cj:18:30:
   | 
18 |         var c: () -> Int64 = {=> d}
   |                              ^^^^^^ 
   | 
note: 'd' is mutable
  ==> function_cannot_capture_var1.cj:18:34:
   | 
18 |         var c: () -> Int64 = {=> d}
   |                                  ^ 
   | 

2 errors generated, 2 errors printed.
*/
