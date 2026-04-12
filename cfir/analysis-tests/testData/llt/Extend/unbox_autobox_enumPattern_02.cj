// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %output

enum MyOption<T> {
    | MyEmpty
    | MyValue(T)
}

extend<T> MyOption<T> <: ToString {
    public func toString() {
        match (this) {
            case MyEmpty => "empty"
            case MyValue(v: ToString) => "MyValue(${v})"
            case MyValue(_) => "cannot print"
        }
    }
}

main(): Int64 {
    let str = MyValue(2).toString()
    if (str != "MyValue(2)") {
        return 1
    }
    return 0
}
