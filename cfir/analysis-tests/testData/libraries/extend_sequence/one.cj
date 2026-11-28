package d7.one

import d7.base.Target

public interface Root {}
public interface Middle <: Root {}

extend Target <: Middle {}
