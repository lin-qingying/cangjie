// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkga

public class A {}

public func C2<T>(): Int32 {
    1
}

public func B<T>(): Int32 {
    return C2<Int32>() + C2<T>()
}
