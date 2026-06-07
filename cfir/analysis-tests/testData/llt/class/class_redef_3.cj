// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class C1 {
    public func foo(): Int64 {
        return 2
    }
}

class C2 <: C1 {
    public <!REDEF_INSTANCE_ERROR, NOTHING_TO_OVERRIDE!>redef<!> func foo(): Int64 {
        return 3
    }
}

main(): Int64 {
    return 1
}
