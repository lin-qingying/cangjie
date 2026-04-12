// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface I1 {
    static func foo(): Int64 {
        return 1
    }
}

interface I2 <: I1 {
    override func foo(): Int64 {
        return 2
    }
}

main(): Int64 {
    return 1
}

/* SCAN
error: non-static member 'foo' cannot have the same name with static member in parent class or interfaces
  ==> interface_no_override_modifier_invalid_2.cj:16:5:
   | 
16 |     override func foo(): Int64 {
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
