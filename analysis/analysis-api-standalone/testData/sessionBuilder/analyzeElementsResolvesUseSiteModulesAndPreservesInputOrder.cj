// MODULE: helper
// FILE: helper.cj
package sample.standalone.elements.helper

class Helper {
}

// MODULE: main(helper)
// MAIN_MODULE
// FILE: main.cj
package sample.standalone.elements.main

import sample.standalone.elements.helper.Helper

func consume(): Helper {
    return Helper()
}
