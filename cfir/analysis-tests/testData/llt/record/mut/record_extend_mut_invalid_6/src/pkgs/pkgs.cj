// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkgs

public struct R1 {
    public var i = 0
}

extend R1 {
    public mut func foo() {
        i = i + 1
    }
}

public let obj = R1()
