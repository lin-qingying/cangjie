// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public open class Base {
    public open func g(): This {
        println("return Base");
        <!TYPE_MISMATCH!>Base().f()<!>
    }
    public func f(): This {
        this
    }
}

public class Sub <: Base {
    private let x: Int64 = 10
    public func subOnly() {
        println("SubOnly: ${x}")
    }
}

main() {
    Sub().g().subOnly()
}
