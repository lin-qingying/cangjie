// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

struct R {
    private var x: Int64 = 2
    public func a() {
        var c: Int32 = 10
        return A()
    }
    private func b() {
        return R().x
    }
}

struct A {
    private func a() {
        var c: Int32 = 10
    }
}

main(): Int64 {
    R().a().a
    R().b

    return 0
}

/* SCAN
error: can not access function 'a'
  ==> record_access_control1.cj:27:5:
   | 
27 |     R().a().a
   |     ^ 
   | 

error: can not access function 'b'
  ==> record_access_control1.cj:28:5:
   | 
28 |     R().b
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
