package e4pkg

import std.sync.*

public let sharedLock: Mutex = ReentrantMutex()
