// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class Base {
    public var b: Int32 = 6
    public init(x: Int32) {
        this.b = 5
    }
}

class <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>Test<!><T> <: Base {
    public var a: Int32 = 3
}
