// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output

func infer<T>(): T {
    throw Exception()
}

public class Bar {
    public operator func +(arg: Bar): Bar {
        Bar()
    }
    public func add(arg: Bar): Bar {
        Bar()
    }
}

func f1(num: Bar): Bar {
    let result = num + infer() // was failed to infer T for operator call without expected type
    return result
}

func f2(num: Bar): Bar {
    num + infer()
}

func f3(num: Bar): Bar {
    num.add(infer())
}

func f4(num: Bar): Bar {
    let result = num.add(infer())
    return result
}

main() {}
