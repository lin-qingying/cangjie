// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package b

import a

public open class My <: a.My {}

public open class A <: a.A<Int64> {
    public open func st(): My {
        My()
    }
}
