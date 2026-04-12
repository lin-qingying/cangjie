// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1 {
    public var a: Int64 = 0

    public func foo1() {
        this.a = 1
    }

    public func foo2() {
        a = 1
    }

    public func foo3() {
        this.a += 1
    }
}

main() {
    return 1
}

/* SCAN
error: instance member variable 'a' cannot be modified in immutable function
  ==> record_mut_invalid_2.cj:13:9:
   | 
13 |         this.a = 1
   |         ^ 
   | 

error: instance member variable 'a' cannot be modified in immutable function
  ==> record_mut_invalid_2.cj:17:9:
   | 
17 |         a = 1
   |         ^ 
   | 

error: instance member variable 'a' cannot be modified in immutable function
  ==> record_mut_invalid_2.cj:21:9:
   | 
21 |         this.a += 1
   |         ^ 
   | 

3 errors generated, 3 errors printed.
*/
