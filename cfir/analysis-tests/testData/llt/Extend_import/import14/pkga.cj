// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg.a

public interface ValueType {
    prop code: String {
        get() {
            "in pkg.a"
        }
    }

    func fCode(): String {
        return "in pkg.a fCode"
    }
}

extend Int64 <: ValueType {}
