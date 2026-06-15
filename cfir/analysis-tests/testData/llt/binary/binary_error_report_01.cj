<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


abstract class Base {
    public static func get(): Int32 {
        return 1
    }
}

class Data <: Base {}

func run(): Int64 {
    var value: Data = Data()
    if (<!OBJECT_CANNOT_ACCESS_STATIC_MEMBER!>v<!>alue.get() != 1) {
        return 1
    }
    return 0
}
