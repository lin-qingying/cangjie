// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

open class C1 {
    public func foo(): Int64 {
        return 0
    }
}

interface I1 {
    func foo(): Int64 {
        return 0
    }
}

class C2 <: C1 & I1 {
    public func foo1(): Int64 {
        var f = super.noexist
        return 0
    }
}

main(): Int64 {
    return 1
}

/* SCAN
error: 'noexist' is not a member of class 'C1'
  ==> class_super_access_invalid_2.cj:23:23:
   | 
23 |         var f = super.noexist
   |                       ^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
