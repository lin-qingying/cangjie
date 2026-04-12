// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class A {
    public let a: Int32 = 0
    public init(aa: Int32) {
        this.a = aa
    }
}

main() {
    let x: Int32 = 10
    var a = A(x)
    return 0
}

/* SCAN
error: cannot assign to immutable value
  ==> let_in_init2.cj:12:9:
   | 
12 |         this.a = aa
   |         ^^^^^^^^^^^ 
   | 
note: variable 'a' is immutable
  ==> let_in_init2.cj:10:16:
   | 
10 |     public let a: Int32 = 0
   |                ^ 
   | 

1 error generated, 1 error printed.
*/
