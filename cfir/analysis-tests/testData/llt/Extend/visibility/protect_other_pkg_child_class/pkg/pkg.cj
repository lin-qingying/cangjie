// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

var g: Int64 = 0

public open class C {}

extend C {
    protected func f() {
        10
    }
    protected mut prop p: Int64 {
        get() {
            g
        }
        set(v) {
            g++
        }
    }
}
