function updateTitle() {
    var current;
    const page = document.documentElement;
    const titles = document.getElementsByClassName("header");
    if (page.clientHeight + page.scrollTop == page.scrollHeight) {
        current =  titles[titles.length - 1]; // 页面滚动到底部时，高亮最后一个标题
    } else {
        current = titles[0];
        for (let title of titles) {
            if (window.pageYOffset + 14 >= title.offsetTop) {
                current = title;
            } else {
                break;
            }
        }
    }
    for (let index of document.getElementsByClassName("pagetoc")[0].children) {
        index.classList.remove("active");
        if (current.href.localeCompare(index.href) == 0) {
            index.classList.add("active");
        }
    }
};

// 在渲染页面之前生成右侧目录，注意不要使用 onload 事件，那样视觉上会有延迟
window.addEventListener('DOMContentLoaded', function() {
    let block = document.createElement("div");
    block.className = "sidetoc";
    let catalog = document.createElement("nav");
    catalog.className = "pagetoc";
    block.appendChild(catalog);
    let content = document.getElementById("content");
    content.insertBefore(block, content.children[1]);

    const titles = document.getElementsByClassName("header");
    for (const title of titles) {
        var link = document.createElement("a");
        if (title.text != '') {
            link.text = title.text
        } else if (title.parentElement.innerText !='') {
            link.text = title.parentElement.innerText;
        }
        link.className = title.parentElement.tagName;
        link.href = title.href;
        catalog.appendChild(link);
    }
    updateTitle();
    // 让标题栏显示子文档名称
    document.getElementsByClassName("menu-title")[0].innerHTML = "API 参考（OHCangjie0.13.3-OH5.0.5.165）";
    // 删除 rust 主题，避免用户误解
    document.getElementById("rust").parentElement.remove();
});

// 在滚动页面时关联当前内容和右侧标题
window.addEventListener("scroll", updateTitle);

// 无连接导航点击展开/收缩目录
var sidebarTitleToggles = document.querySelectorAll('#sidebar .chapter-item div');

function toggleSection(ev) {
    ev.currentTarget.parentElement.classList.toggle('expanded');
}

Array.from(sidebarTitleToggles).forEach(function (el) {
    el.addEventListener('click', toggleSection);
});
