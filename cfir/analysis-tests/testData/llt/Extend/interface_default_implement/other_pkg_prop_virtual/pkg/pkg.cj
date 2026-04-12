// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public interface I {
    prop p: Int64 {
        get() {
            return h1() + h2()
        }
    }
    func h1(): Int64
    func h2(): Int64
}
