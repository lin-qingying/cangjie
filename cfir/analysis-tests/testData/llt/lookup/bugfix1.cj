public interface Schedule{}

public interface Job{
    func Run():Unit
}

class EntryID{}

public class Entry{
    public let <!sema_accessibility!>ID<!>: EntryID
    public var Schedule:Schedule
    public var WrappedJob:<!sema_not_a_type!>J<!>ob
    public var Job:Job
    init(id:EntryID,s:<!sema_not_a_type!>S<!>chedule,w:<!sema_not_a_type!>J<!>ob,j:<!sema_not_a_type!>J<!>ob){
        this.ID = id
        this.Schedule = s
        this.Job = j
        this.WrappedJob = w

    }
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
