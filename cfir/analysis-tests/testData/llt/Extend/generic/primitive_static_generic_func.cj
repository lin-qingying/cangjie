// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

extend Int64 {
    static func foo<T>() {
        1
    }
}

main() {
    var a = Int64.foo<Int64>
    return a() - 1
}
