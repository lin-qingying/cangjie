public interface Schedule{}

public interface Job{
    func Run():Unit
}

class EntryID{}

public class Entry{
    public let <!ACCESSIBILITY_ERROR!>ID<!>: EntryID
    public var Schedule:Schedule
    public var WrappedJob:<!NOT_A_TYPE!>J<!>ob
    public var Job:Job
    init(id:EntryID,s:<!NOT_A_TYPE!>S<!>chedule,w:<!NOT_A_TYPE!>J<!>ob,j:<!NOT_A_TYPE!>J<!>ob){
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
