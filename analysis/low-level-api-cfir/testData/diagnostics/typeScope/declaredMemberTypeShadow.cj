// 与仓库 lookup/bugfix1 的类体成员遮蔽形状一致。
public interface Schedule {}
public interface Job {
    func Run(): Unit
}
class EntryID {}
public class Entry {
    public let ID: EntryID
    public var Schedule: Schedule
    public var WrappedJob: Job
    public var Job: Job
    init(id: EntryID, s: Schedule, w: Job, j: Job) {
        this.ID = id
        this.Schedule = s
        this.Job = j
        this.WrappedJob = w
    }
}
