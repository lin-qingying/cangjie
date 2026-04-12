// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class A {
    public static let b: Float32 = 0.0
    public A(bb: Float32) {
        A.b = bb
    }
}

main() {
    let y: Float32 = 9.0
    var a = A(y)
    return 0
}

/* SCAN
error: cannot assign to immutable value
  ==> let_in_init7.cj:12:9:
   | 
12 |         A.b = bb
   |         ^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init7.cj:10:23:
   | 
10 |     public static let b: Float32 = 0.0
   |                       ^ 
   | 

1 error generated, 1 error printed.
*/
