// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

type E<T> = Option<T>

main() {
    let a: ?Int64 = E.None
    let b = Some<?Int64>(E.None)
    return 0
}
