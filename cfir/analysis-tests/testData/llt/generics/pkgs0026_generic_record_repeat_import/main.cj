// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkga
// DEPENDENCE: pkgb

internal import pkga.*
internal import pkgb.*

main(): Int64 {
    let ans: R<Int32> = a
    return Int64(ans.a - 1)
}
