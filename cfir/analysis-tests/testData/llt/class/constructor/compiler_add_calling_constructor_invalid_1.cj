// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class C1 {
    private init() {
    }
}

class C2 <: C1 {
    <!EXPLICIT_SUPER_CALL_REQUIRED!>public init()<!> {
    }
}

main() {
    return 0
}
