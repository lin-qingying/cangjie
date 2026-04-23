internal import std.collection.*
internal import std.time.*
internal import std.sync.*

class LockInfo {
    var count = Int32(1)
    var time = DateTime.now().toUnixTimeStamp().toSeconds()
}

public class IpLockMgr  {
    var ipInfo = HashMap<String, LockInfo>()
    var locker = ReentrantMutex()
    var failedCount = Int32(0)
    var failedInterval = Int64(0)
    var lockedExpired = Int64(0)

    init () {
        failedCount = 3
        failedInterval = 10
        lockedExpired = 30
    }
    // when login failed, update failed ip
    public func recordFailedInfo(ip: String) {
        locker.lock()
        // check failed info
        var info = ipInfo.get(ip)
        if (let Some(lockinfo) <- info) {
            lockinfo.count += 1
            lockinfo.time = DateTime.now().toUnixTimeStamp().toSeconds()
        } 
        locker.unlock()
    }
}

main() {
    0
}
