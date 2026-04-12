// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    public var a: Int64 = 1
    public init() {
        var b: Int64 = 4
        func f() {
            b
        }
        func g() {
            var a = {=> f}
            return 0
        }
    }
}

/* SCAN
error: function capturing mutable variables needs to be called directly
  ==> capture3.cj:17:25:
   | 
17 |             var a = {=> f}
   |                         ^ 
   | 
note: 'b' is mutable
  ==> capture3.cj:14:13:
   | 
14 |             b
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
