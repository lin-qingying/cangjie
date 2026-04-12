// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public interface innerInterface {
    static func staticFunc(): Int32 {
        0
    }
}

public open class innerClass <: innerInterface {
    public static func staticFunc(): Int32 {
        0
    }
}
