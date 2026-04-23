// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class C1<T> {
    public open func foo(): C1<T> {
        return this
    }
}

class C2<T> <: C1<T> {
    public override func foo(): This {
        return this
    }
}

main() {
    return 0
}
