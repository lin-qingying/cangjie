// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class A {
    public static let b: Float32
    public init(bb: Float32) {
        A.b = bb
    }
}

main() {
    let y: Float32 = 9.0
    var a = A(y)
    return 0
}

/* SCAN
error: the static member variable 'b' is not initialized
  ==> let_in_init3.cj:10:5:
   | 
10 |     public static let b: Float32
   |     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: you may add an initializer inline or define a 'static init' to perform initialization

error: cannot assign to immutable value
  ==> let_in_init3.cj:12:9:
   | 
12 |         A.b = bb
   |         ^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init3.cj:10:23:
   | 
10 |     public static let b: Float32
   |                       ^ 
   | 

2 errors generated, 2 errors printed.
*/
