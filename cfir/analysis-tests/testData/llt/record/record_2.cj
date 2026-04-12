// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct Data {
    public let a = 1
    public let b = 2
    public init(x: Int64, y: Int64) {
        a = x
        b = y
    }
}

main(): Int64 {
    return run()
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
