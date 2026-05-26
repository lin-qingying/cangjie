// MODULE: helper
// FILE: helper.cj
package sample.standalone.priority.helper

class Helper {
}

func helperFactory(): Helper {
    return Helper()
}

// MODULE: main(helper)
// MAIN_MODULE
// MAIN_FILE_NAME: broad.cj
// FILE: broad.cj
package sample.standalone.priority.main

import sample.standalone.priority.helper.helperFactory

func broadUsage() {
    helperFactory()
}

// FILE: specific.cj
package sample.standalone.priority.main

import sample.standalone.priority.helper.helperFactory

func specificUsage(): sample.standalone.priority.helper.Helper {
    return helperFactory()
}
