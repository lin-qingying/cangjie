// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg1

public interface MyInterface {
    static func f<T>(a: T): T {
        a
    }
    func g(): Int64 {
        f<Int64>(1)
    }
}

extend Int64 <: MyInterface {}

public class A {}

extend A <: MyInterface {}
