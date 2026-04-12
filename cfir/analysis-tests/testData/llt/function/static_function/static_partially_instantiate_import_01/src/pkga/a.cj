// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkga

public class Boo<T> {
    public static func foo<V>(x: T, y: V) {
        return y
    }
}

func foo() {
    return Boo<Int32>.foo<Int64>(1, 0)
}
