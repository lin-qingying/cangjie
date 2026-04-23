// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return 0
}

func f(): Int32 {
    return 0
}

class A {
    public let a: () -> Int32
    public init(b: Int32) {
        a = f
    }
}
