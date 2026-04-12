// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg1

public interface I0 {
    func test(): UInt64
}

public interface I1 <: I0 {
    func test(): UInt64 {
        return 1
    }
}

public class A {}

extend A <: I1 {
    public func test(): UInt64 {
        return 2
    }
}
