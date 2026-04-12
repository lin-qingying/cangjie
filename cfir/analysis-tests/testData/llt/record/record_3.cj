// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct RA{
    public var a
    public init(input:Int32){
        a = input
    }
}

main(): Int64 {
    var l_a = 4
    var r_a = RA(2)
    l_a = r_a.a
    if (l_a == Int32(2)) {
        return 0
    }
    return 1
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace