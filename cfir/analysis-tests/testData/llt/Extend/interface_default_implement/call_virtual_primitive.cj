// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    func h1(): Int64
    func h2(): Int64 {
        h1()
    }
}

extend Int32 <: I {
    public func h1(): Int64 {
        1
    }
}

main() {
    return 1.h2() - 1
}
