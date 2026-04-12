// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1<T> {
    public var a: T

    public func foo1(x: T) {
        this.a = x
    }

    public func foo2(x: T) {
        a = x
    }
}

main() {
    return 1
}

/* SCAN
error: instance member variable 'a' cannot be modified in immutable function
  ==> record_mut_invalid_3.cj:13:9:
   | 
13 |         this.a = x
   |         ^ 
   | 

error: instance member variable 'a' cannot be modified in immutable function
  ==> record_mut_invalid_3.cj:17:9:
   | 
17 |         a = x
   |         ^ 
   | 

error: the uninitialized member variable 'a' is not initialized in the constructor of class or struct
  ==> record_mut_invalid_3.cj:10:5:
   | 
10 |     public var a: T
   |     ^ 
   | 

3 errors generated, 3 errors printed.
*/
