// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I1 {
    func foo(): Int64 {
        return 1
    }
}

interface I2 <: I1 {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>s<!>tatic func foo(): Int64 {
        return 2
    }
}

main(): Int64 {
    return 1
}
