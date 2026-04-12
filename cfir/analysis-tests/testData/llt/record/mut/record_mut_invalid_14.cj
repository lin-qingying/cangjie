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
    }

    public func goo1() {
        return foo
    }

    public func goo2() {
        return foo()
    }
}

interface I1 {
    mut func foo(): Unit {
    }

    func goo1() {
        return foo
    }

    func goo2() {
        return foo()
    }
}

main() {
    return 1
}

/* SCAN
error: immutable function 'goo1' cannot access mutable function 'foo'
  ==> record_mut_invalid_14.cj:17:16:
   | 
17 |         return foo
   |                ^ 
   | 

error: immutable function 'goo2' cannot access mutable function 'foo'
  ==> record_mut_invalid_14.cj:21:16:
   | 
21 |         return foo()
   |                ^ 
   | 

error: immutable function 'goo1' cannot access mutable function 'foo'
  ==> record_mut_invalid_14.cj:30:16:
   | 
30 |         return foo
   |                ^ 
   | 

error: immutable function 'goo2' cannot access mutable function 'foo'
  ==> record_mut_invalid_14.cj:34:16:
   | 
34 |         return foo()
   |                ^ 
   | 

4 errors generated, 4 errors printed.
*/
