// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package example.data

protected struct Data {
    protected init() {}
    protected func foo() {}
    protected var a_ = 0
    protected prop a: Int64 {
        get() {
            a_
        }
    }
}
