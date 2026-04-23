// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I<T> {}

class A<T> <: I<Int32> {}

extend<T> A<T> <: I<Int64> {}

main() {
    return 0
}
