// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

abstract class C1 {
    public var a: Int32 = 5
    public func f(): Int32
}

open class C2 <: C1 {
    public func f(): Int32 {
        return 5
    }
}

class C3 <: C2 {
    public var k: Int32
    public init() {
        k = super.f()
    }
    public func getK(): Int32 {
        return k
    }
}

main() {
    var ins = C3()
    if (ins.getK() == 5) {
        return 0
    }
    return 1
}

/* SCAN
error: 'f' is not allowed to be accessed before all member variables are initialized
  ==> super_this_06.cj:23:13:
   | 
23 |         k = super.f()
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
