// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f() {
    print("func")
    {arr: Array<Int64> => arr.size}
}

func g() {
    print("arg")
    0
}

main() {
    g() |> f()
}
