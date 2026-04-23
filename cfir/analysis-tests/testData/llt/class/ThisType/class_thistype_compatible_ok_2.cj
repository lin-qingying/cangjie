// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class C1 {
    public open func foo(): C1 {
        return this
    }
}

class C2 <: C1 {
    public override func foo(): This {
        return this
    }
}

main() {
    return 0
}
