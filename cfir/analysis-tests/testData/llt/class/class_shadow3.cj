// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

open class Base {
    public let val: Int32 = 0
}

class Data <: Base {
    public let val: Int32 = 1
}

open class DataI {
    public static let val: Int32 = 4
}

class DataIDerived <: DataI {
    public let val: Int32 = 5
}

main(): Int64 {
    var objBase = Base()
    var objData = Data()
    var res0: Int32 = objBase.val
    var res1: Int32 = objData.val
    var res4: Int32 = DataI.val
    var objDataIDerived = DataIDerived()
    var res5: Int32 = objDataIDerived.val
    if (res0 != 0 || res1 != 1 || res4 != 4 || res5 != 5) {
        return 1
    }
    return 0
}

/* SCAN
error: the variable 'val' must not shadow a member variable of the supertype
  ==> class_shadow3.cj:14:5:
   | 
14 |     public let val: Int32 = 1
   |     ^ 
   | 

error: non-static member 'val' cannot have the same name with static member in parent class or interfaces
  ==> class_shadow3.cj:22:5:
   | 
22 |     public let val: Int32 = 5
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
