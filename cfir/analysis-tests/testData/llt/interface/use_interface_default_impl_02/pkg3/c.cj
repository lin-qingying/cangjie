// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg3

internal import pkg1.*
internal import pkg2.*

public func call<T>(): Int64 {
    if (myClass<Int32>.foo2<() -> Int64>(a: myClass<Int32>.foo1())() == 1 && myClass<Int32>.foo3()() == 1 &&
        myClass<Int32>.x == 20) {
        return 0
    } else {
        return 1
    }
}
