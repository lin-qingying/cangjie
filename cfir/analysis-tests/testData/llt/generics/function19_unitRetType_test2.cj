// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func x(input: Unit): Unit {
    input
}

func y(input: Unit): Unit {
    input
}

main(): Int64 {
    (x ~> y)
    return 0
}
