// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

struct A {
    public let a: Int64 = 1
    public let b: Int64 = 2
    private init(s: String) {
        this.a = s.size
    }
}
