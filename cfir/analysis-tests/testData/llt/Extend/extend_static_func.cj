// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

extend Int32 {
    public static func sub(a: Int64): Int64 {
        return a - 1
    }
}

main() {
    let a: Int64 = 1
    return Int32.sub(a)
}
