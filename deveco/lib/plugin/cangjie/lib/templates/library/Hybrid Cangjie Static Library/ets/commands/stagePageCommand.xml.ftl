<#if uiSyntax?lower_case == "ets">
    <instantiate src="ets/code template/src/common/traditionalCode/ets/ets/main_page/main_page.ets.ftl"
          dest="${rootOut}/src/main/ets/${pageMidDir}/${pagePackageName}.ets" />
    <open file="${rootOut}/src/main/ets/${pageMidDir}/${pagePackageName}.ets"/>
</#if>