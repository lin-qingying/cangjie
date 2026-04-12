// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

open class Base {
    public let val: Int32 = 0
}

class Data <: Base {
    public let val: Int32 = 1
    public let f: Int32 = super.val
}

main(): Int64 {
    var data = Data()
    var d = data.val
    var f = data.f
    if (d != 1 || f != 0) {
        return 1
    }
    return 0
}

/* SCAN
error: 'super' is not allowed to initialize non-static member
  ==> class_shadow9.cj:15:27:
   | 
15 |     public let f: Int32 = super.val
   |                           ^ 
   | 

error: the variable 'val' must not shadow a member variable of the supertype
  ==> class_shadow9.cj:14:5:
   | 
14 |     public let val: Int32 = 1
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
