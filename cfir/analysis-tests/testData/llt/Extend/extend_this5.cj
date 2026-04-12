// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

extend Int64 {
    public static func foo() {
        return this
    }
}

class A {
    public func f() {}
}

extend A {
    public static func foo() {
        return this.f()
    }
}

struct B {
    public let item: Int64 = 0
}

extend B {
    public static func foo() {
        return this.item
    }
}

/* SCAN
error: non-static variable 'this' cannot be referenced from a static context
  ==> extend_this5.cj:11:16:
   | 
11 |         return this
   |                ^ 
   | 

error: non-static variable 'this' cannot be referenced from a static context
  ==> extend_this5.cj:21:16:
   | 
21 |         return this.f()
   |                ^ 
   | 

error: non-static variable 'this' cannot be referenced from a static context
  ==> extend_this5.cj:31:16:
   | 
31 |         return this.item
   |                ^ 
   | 

error: 'main' is missing
 ==> extend_this5.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

4 errors generated, 4 errors printed.
*/
