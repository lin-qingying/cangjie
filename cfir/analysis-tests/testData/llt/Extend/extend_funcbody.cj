// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A{
    public var a = 0
}

extend A {
    public func foo1():Int64<!parse_missing_body!> <!>
}

main(): Int64 {
    return A().foo1()
}
