package e4pkg

func useLock(): Unit {
    synchronized(sharedLock) {
        let _ = 1
    }
}
