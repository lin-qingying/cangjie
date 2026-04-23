// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    public var a: Int64 = 1
    public init() {
        var b: Int64 = 4
        func f() {
            b
        }
        func g() {
            var a = {=> <!USE_FUNC_CAPTURE_VAR_ALONE!>f<!>}
            return 0
        }
    }
}
