// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f<T>(a: Option<T>) {}

main() {
    f<Int64>(None) // ok
    f(Option<Int64>.None) // ok

    f<Nothing>(None) // ok

    f(Option<Nothing>.None)
}
