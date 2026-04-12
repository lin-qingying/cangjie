// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum EE {
    A

    public prop a1: Int64 {
        get() {
            2
        }
    }
    public static prop a2: Int64 {
        get() {
            2
        }
    }
    public func test() {}
    public static func test1() {}
}

main(): Int64 {
    EE.a1
    EE.A
    EE.a2
    EE.test()
    EE.test
    EE.test1
    EE.test1()
    0
}

/* SCAN
error: 'a1' is non-static member, cannot access by type name
  ==> enum29.cj:27:5:
   | 
27 |     EE.a1
   |     ^ 
   | 

error: 'test' is non-static member, cannot access by type name
  ==> enum29.cj:30:5:
   | 
30 |     EE.test()
   |     ^ 
   | 

error: 'test' is non-static member, cannot access by type name
  ==> enum29.cj:31:5:
   | 
31 |     EE.test
   |     ^ 
   | 

3 errors generated, 3 errors printed.
*/
