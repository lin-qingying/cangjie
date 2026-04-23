// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct R<T> {}

extend<T> R<T> {
    public static func f(): Int64 {
        g<Rune>()
    }
    public static func g<X>(): Int64 {
        1
    }
}

main(): Int64 {
    return R<Int8>.f() - 1
}
