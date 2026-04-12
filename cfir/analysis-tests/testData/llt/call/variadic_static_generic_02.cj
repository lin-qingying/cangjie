// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

public class Choir<T> {}

public interface JustExtension {}

extend<T> Choir<T> <: JustExtension {
    public static func just(arr: Array<T>): Choir<T> {
        return Choir<T>()
    }
}

public class Person {}

public func getChoirPerson(p: Person): Choir<Person> {
    return Choir.just(p)
}

main() {}
