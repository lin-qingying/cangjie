// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class ObservableCreate<T> {}

class Observable<T> {
    public static func create<U>(a: U): ObservableCreate<U> {
        return ObservableCreate<U>()
    }
}

main() {
    let obj = Observable<Bool>.create<Int64>(1)
    return 0
}
