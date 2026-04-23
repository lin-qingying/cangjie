// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: a.cj

import pkga.foo

class B {
    <!TYPE_UNINITIALIZED_STATIC_FIELD!>public static let CTX: Int64<!>

    static init() {
        foo(
            {
                =>
                    CTX = 10
                    println(CTX)
            }
        )
    }
}

main() {}
