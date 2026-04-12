// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg1

internal import pkg0.*

public interface I {
    func fi(): Int64
}

public interface J {
    func fj(): Int64
}

extend A <: I & J {
    public func fi() {
        1
    }
    public func fj() {
        2
    }
}
