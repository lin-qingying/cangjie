// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public interface I {
    static func f(): Int64 {
        1
    }
}

public open class A {
    public static func f(): Int64 {
        0
    }
}

public class B <: A {}

extend B <: I {}
