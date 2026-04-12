// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package test.a

class MyListIterator<T> <: Iterator<T> {
    @Frozen
    @OverflowWrapping
    public func next(): Option<T> {
        return None
    }
}
public class MyList<T> <: Iterable<T> {
    public func fold(): Unit {}
    public func reduce(): Unit {}
    @Frozen
    public func iterator(): Iterator<T> {
        println("call iterator")
        return MyListIterator<T>()
    }
    @Frozen
    public func isEmpty(): Bool {
        return false
    }
}

extend<T> MyList<T> <: ToString where T <: ToString {
    @Frozen
    public func toString(): String {
        return collectionToString<MyList<T>, T>(this)
    }
}

@Frozen
func collectionToString<C, T>(col: C): String where C <: Iterable<T>, T <: ToString {
    col.iterator()
    return ""
}