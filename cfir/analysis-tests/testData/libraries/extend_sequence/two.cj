package d7.two

import d7.base.Target
import d7.one.Middle
import d7.one.Root

public interface Deep <: Middle {}

extend Target <: Root & Deep {}
