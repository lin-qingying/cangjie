// ERRCHECK: %compiler %cmp_opt %f -o %output


public interface Schedule{}

public interface Job{
    func Run():Unit
}

class EntryID{}

public class Entry{
    public let ID: EntryID
    public var Schedule:Schedule
    public var WrappedJob:Job
    public var Job:Job
    init(id:EntryID,s:Schedule,w:Job,j:Job){
        this.ID = id
        this.Schedule = s
        this.Job = j
        this.WrappedJob = w

    }
}

/* SCAN
error: 'Job' is not a type
  ==> bugfix1.cj:15:27:
   | 
15 |     public var WrappedJob:Job
   |                           ^ 
   | 

error: 'Schedule' is not a type
  ==> bugfix1.cj:17:23:
   | 
17 |     init(id:EntryID,s:Schedule,w:Job,j:Job){
   |                       ^ 
   | 

error: 'Job' is not a type
  ==> bugfix1.cj:17:34:
   | 
17 |     init(id:EntryID,s:Schedule,w:Job,j:Job){
   |                                  ^ 
   | 

error: 'Job' is not a type
  ==> bugfix1.cj:17:40:
   | 
17 |     init(id:EntryID,s:Schedule,w:Job,j:Job){
   |                                        ^ 
   | 

error: 'public' declaration uses 'internal' types
  ==> bugfix1.cj:13:16:
   | 
13 |     public let ID: EntryID
   |                ^^ 
   | 
note: type 'Class-EntryID' contains 'internal' type
  ==> bugfix1.cj:13:20:
   | 
13 |     public let ID: EntryID
   |                    ^^^^^^^ 
   | 
note: the 'internal' type is 'Class-EntryID'
  ==> bugfix1.cj:10:7:
   | 
10 | class EntryID{}
   |       ^^^^^^^ 
   | 

error: 'main' is missing
 ==> bugfix1.cj:1:1:
  | 
1 | // ERRCHECK: %compiler %cmp_opt %f -o %output
  | ^ 
  | 

6 errors generated, 6 errors printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
