// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

open class Base {
    public static let val: Int32 = 0
    public static func f(): Int32 {
        return 0
    }
}

class Data <: Base {
    public let val: Int32 = 1
    public let f: Int32 = 2
}

main(): Int64 {
    var data = Data()
    var d = data.val
    var f = data.f
    if (d != 1 || f != 2) {
        return 1
    }
    return 0
}

/* SCAN
error: non-static member 'val' cannot have the same name with static member in parent class or interfaces
  ==> class_shadow8.cj:17:5:
   | 
17 |     public let val: Int32 = 1
   |     ^ 
   | 

error: non-static member 'f' cannot have the same name with static member in parent class or interfaces
  ==> class_shadow8.cj:18:5:
   | 
18 |     public let f: Int32 = 2
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
