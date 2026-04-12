// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package b

public struct A {
    private var val = 1
    public mut prop a: Int64 {
        get() {
            return val
        }
        set(v) {
            val = v
        }
    }
}
