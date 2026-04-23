// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var i: Int64 = 1

    var a: Array<Array<Int64>> = Array<Array<Int64>>(i, Array<Int64>(i, <!TYPE_MISMATCH!>i<!>))

    return 0
}
