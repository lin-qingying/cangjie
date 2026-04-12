// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I0 {
    func foo(): Int64 {
        return 0
    }
}

class C0 <: I0 {
    public func foo2(): Int64 {
        var res: Int64 = super.foo1()
        return res
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: 'foo1' is not a member of class 'Object'
  ==> class_super_member_func_call_invalid_2.cj:17:32:
   | 
17 |         var res: Int64 = super.foo1()
   |                                ^^^^ 
   | 

1 error generated, 1 error printed.
*/
