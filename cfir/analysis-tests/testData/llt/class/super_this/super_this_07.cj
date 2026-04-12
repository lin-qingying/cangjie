// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

abstract class FaTest {
    public var a: Int32 = 5
    public func f(): Int32
}

class Test1 <: FaTest {
    public var fPtr: () -> Int32
    public init(x: Int32) {
        a = x
        fPtr = super.f
    }
    public func f(): Int32 {
        return a
    }
    public func get(): Int32 {
        return fPtr()
    }
}

main() {
    var ins = Test1(15)
    print("${ins.get()}\n")
    return 0
}

/* SCAN
error: abstract method 'f' cannot be accessed directly
  ==> super_this_07.cj:18:16:
   | 
18 |         fPtr = super.f
   |                ^ 
   | 

1 error generated, 1 error printed.
*/
