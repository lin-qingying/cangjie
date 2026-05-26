// MODULE: helper
// FILE: helper.cj
package sample.standalone.order.helper

class Helper {
}

// MODULE: main(helper)
// MAIN_MODULE
// MAIN_FILE_NAME: entry.cj
// FILE: entry.cj
package sample.standalone.order.main

import sample.standalone.order.helper.Helper

func entry(): Helper {
    return Helper()
}

// FILE: secondary.cj
package sample.standalone.order.main

class Secondary {
}
