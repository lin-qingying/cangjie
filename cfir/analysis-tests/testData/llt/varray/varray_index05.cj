// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var varr: VArray<Int64, $9223372036854775808> = VArray<Int64, $9223372036854775808> {i => i + 1}
    return varr[0] - 1
}
