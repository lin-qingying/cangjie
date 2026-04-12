// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// UpperBound: rule 3: If there are multiple classes then these classes, they must be in one inheritance chain.

open class Base {}

class Child <: Base {}

func foo<T>() where T <: Base & Child {}

main(): Int64 {
    return 0
}
