import testNapi from 'lib${moduleName?lower_case}.so'

export default {
    data: {
        title: "",
        myText: ""
    },
    onInit() {
        this.title = this.$t('strings.world');
    },
    onClick() {
        this.myText = testNapi.add(2, 3);
    }
}