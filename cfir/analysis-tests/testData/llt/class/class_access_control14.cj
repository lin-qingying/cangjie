// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return Int64(Data().foo())
}

abstract class Base {
    protected func foo(): Int32
}

class Data <: Base {
    public func foo(): Int32 {
        return 0
    }
}
