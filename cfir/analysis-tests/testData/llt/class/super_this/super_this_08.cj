// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface Base {
    func f1(): Int32
    func f2(): Int32 {
        return f1()
    }
}

class Data <: Base {
    public var m: Int32
    public init() {
        m = f2()
    }

    public func f1(): Int32 {
        return m
    }
}

/* SCAN
error: 'f2' is not allowed to be accessed before all member variables are initialized
  ==> super_this_08.cj:19:13:
   | 
19 |         m = f2()
   |             ^ 
   | 

error: 'main' is missing
 ==> super_this_08.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
