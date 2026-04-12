// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

abstract class FaTest {
    public var a: Int32 = 1
    public func f(): Int32
}

class Test1 <: FaTest {
    public init(x: Int32) {
        a = super.f()
    }
    public func f(): Int32 {
        return 5
    }
}

main() {
    return 0
}

/* SCAN
error: abstract method 'f' cannot be accessed directly
  ==> super_this_01.cj:16:13:
   | 
16 |         a = super.f()
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
