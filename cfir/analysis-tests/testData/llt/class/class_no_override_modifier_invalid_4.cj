// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface I1 {
    func foo(): Int64 {
        return 1
    }
}

class C1 <: I1 {
    public static func foo(): Int64 {
        return 2
    }
}

main(): Int64 {
    return 1
}

/* SCAN
error: static member 'foo' cannot have the same name with non-static member in parent class or interfaces
  ==> class_no_override_modifier_invalid_4.cj:16:5:
   | 
16 |     public static func foo(): Int64 {
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
