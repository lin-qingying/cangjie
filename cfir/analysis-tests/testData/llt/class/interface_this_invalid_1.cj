// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I0 {
    static func foo(): Int64 {
        return 1
    }

    func foo1(): Int64 {
        var res = this.foo()
        return res
    }
}

class C0 <: I0 {}

main(): Int64 {
    return 0
}

/* SCAN
error: object cannot access static member 'foo'
  ==> interface_this_invalid_1.cj:15:19:
   | 
15 |         var res = this.foo()
   |                   ^ 
   | 

1 error generated, 1 error printed.
*/
