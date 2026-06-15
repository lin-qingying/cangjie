// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    <!IGNORE_OPEN!>p<!>ublic open func f(): Int32 {
        return 0
    }

    public init() {
        f()
        var a = {=> f}
        <!chir_dce_unused_function!>func f3(<!chir_dce_unused_variable!>c<!>!: Int32 = f()) {
            f
        }<!>
        a()
    }
}

main(): Int64 {
    0
}
