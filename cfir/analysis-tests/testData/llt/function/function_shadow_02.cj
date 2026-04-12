// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

class A {
    operator func ()(a: Rune) {}
}

struct S {
    func C(a: Rune): String {
        "ok"
    }
    func test() {
        var C: A = A() // This variable shadows the outer C function at line 14, so only one candidate for line 20
        func foo(a: Int8) {
            func C(a: Int8) {} // This function shadows the outer C variable at line 18
            let v: Rune = 'a'
            let str: String = C(v)
        }
    }
}

/* SCAN
error: mismatched types
  ==> function_shadow_02.cj:22:33:
   | 
22 |             let str: String = C(v)
   |                                 ^ expected 'Int8', found 'Rune'
   | 

1 error generated, 1 error printed.
*/
