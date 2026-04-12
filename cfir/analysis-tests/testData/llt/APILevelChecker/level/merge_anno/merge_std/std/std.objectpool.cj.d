// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.



package std.objectpool
import std.collection.concurrent.*

import std.ref.*
import std.collection.LinkedList

@Deprecated
@!APILevel[since: "12", atomicservice : true]
public class ObjectPool<T> where T <: Object {
    @!APILevel[since: "12", atomicservice : true]
    public init(newFunc: () -> T, resetFunc!: Option<(T) -> T> = None)
    
    @!APILevel[since: "12", atomicservice : true]
    public func get(): T
    
    @!APILevel[since: "12", atomicservice : true]
    public func put(item: T): Unit
}

