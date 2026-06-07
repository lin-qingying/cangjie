// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func a(input: Int64): Int64 {
    input
}

main(): Int64 {
    var <!chir_dce_unused_variable!>x<!>: (Int64) -> Int64 = a ~> a
    return 0
}
