// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func f<T>(a: Any, t: T): String {
    return ""
}

func f2<T>(a: Any, t: T): T {
    return t
}

main(): Int64 {
    f("", "")
    f2("", 0)
}
