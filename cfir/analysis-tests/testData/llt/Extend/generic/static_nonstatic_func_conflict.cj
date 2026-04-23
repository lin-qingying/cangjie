// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {}

extend A {
    public static func <!STATIC_FUNCTION_OVERLOAD_CONFLICTS!>f<!>(a: Int8) {}
    public func f() {}
}
