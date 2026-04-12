import std.deriving.Derive

@Derive[Equatable]
class B{
    var a = 1
    init(temp:Int64){
        a = temp
    }
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
