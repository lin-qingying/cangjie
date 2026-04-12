// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

public interface NoneTrait {
    func IsNone(): Bool
}

extend<T> Option<T> <: NoneTrait {
    public func IsNone(): Bool
    {
        return match (this) {
            case Some(v) => false
            case None => true
        }
    }
}

main() {
    let b = None<Int64>
    if (!b.IsNone()) {
        return 1
    }
    return 0
}



// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
