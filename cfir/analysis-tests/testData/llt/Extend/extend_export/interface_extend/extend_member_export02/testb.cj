// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package b

import a.*

public interface I {
    public prop f1: Int64 {
        get() {
            1
        }
    }
    public prop f3: Int64
}

extend A <: I {
    public prop f2: Int64 {
        get() {
            2
        }
    }
    public prop f3: Int64 {
        get() {
            3
        }
    }
}
