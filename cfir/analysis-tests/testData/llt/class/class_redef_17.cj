// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %f 2>&1

let var1: Class25<Enum319> = Class25<Enum319>()
let var2: Class50<Enum319> = Class50<Enum319>()

open class Class25<T1> {
    public static func func501(): Unit {
    }
}

public open class Class50<T1> <: Class25<Enum319> {
    public static func func501(): Unit {
    }
}

enum Enum319 {
    defaultValue
}

main(): Int64 {
    return 0
}
