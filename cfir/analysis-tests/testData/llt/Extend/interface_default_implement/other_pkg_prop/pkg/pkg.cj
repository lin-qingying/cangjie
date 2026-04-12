// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

var g = 0

public interface I {
    mut prop p: Int64 {
        get() {
            return g
        }
        set(v) {
            g++
        } // side effect.
    }
}
