// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1 {
    public var i = 0

    public mut func foo(): Unit {
        i += 1
        hoo()
    }

    public func goo(): Unit {
        foo()
    }

    public func hoo(): Unit {
    }
}

interface I1 {
    mut func foo(): Unit {
        hoo()
    }

    func goo(): Unit {
        foo()
    }

    func hoo(): Unit {
    }
}

main() {
    return 1
}

/* SCAN
error: immutable function 'goo' cannot access mutable function 'foo'
  ==> record_mut_invalid_12.cj:18:9:
   | 
18 |         foo()
   |         ^ 
   | 

error: immutable function 'goo' cannot access mutable function 'foo'
  ==> record_mut_invalid_12.cj:31:9:
   | 
31 |         foo()
   |         ^ 
   | 

2 errors generated, 2 errors printed.
*/
