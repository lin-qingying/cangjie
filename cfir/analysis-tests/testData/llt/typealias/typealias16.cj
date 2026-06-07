// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A<T> {}

type A1<T> = A<T>

type A2<T> = A1<T>

main() {
    let a = A2<Int64>()
    0
}
