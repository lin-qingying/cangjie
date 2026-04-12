// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum ET {
    A
    | B(String)
    | C(String, String)

    public func foo1(): Int64 {
        print("function foo1 defined in enum\n")
        return 1
    }

    public func foo1(a: Int64): Int64 {
        print("function foo1(Int64) defined in enum\n")
        return 1
    }

    private func foo2(): Int64 {
        print("function foo2 defined in enum\n")
        return 1
    }

    public static func foo3(): Int64 {
        print("function foo3 defined in enum\n")
        return 1
    }
}

main() {
    let e1: ET = ET.A
    let e2: ET = ET.B("one")
    e1.foo2()
    e2.foo3()
    ET.foo1()
    ET.foo1(1)

    return 0
}

/* SCAN
error: no matching function declaration for function call 'foo2'
  ==> enum16_memberFunc.cj:38:8:
   | 
38 |     e1.foo2()
   |        ^ 
   | 

error: object cannot access static member 'foo3'
  ==> enum16_memberFunc.cj:39:5:
   | 
39 |     e2.foo3()
   |     ^ 
   | 

error: 'foo1' is non-static member, cannot access by type name
  ==> enum16_memberFunc.cj:40:5:
   | 
40 |     ET.foo1()
   |     ^ 
   | 

error: 'foo1' is non-static member, cannot access by type name
  ==> enum16_memberFunc.cj:41:5:
   | 
41 |     ET.foo1(1)
   |     ^ 
   | 

4 errors generated, 4 errors printed.
*/
