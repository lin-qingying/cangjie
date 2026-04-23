/**
 * @see https://github.com/HillLiu/docker-mdbook
 */
window.elasticlunr.Index.load = (index) => {
  const FzF = window.fzf.Fzf;
  const storeDocs = index.documentStore.docs;
  const indexArr = Object.keys(storeDocs);
  const ofzf_v1 = new FzF(indexArr, {
    fuzzy: false,
    selector: (item) => {
      const res = storeDocs[item];
      // 把所有文本改成小写,进行模糊匹配
      res.text = `${res.title}${res.breadcrumbs}${res.body}`.toLowerCase();
      return res.text;
    },
  });

  return {
    search: (searchterm) => {
      // 关键字也全部改成小写
      searchterm = searchterm.trim().replace(/\s+/g, " ").toLowerCase();
      const entries = ofzf_v1.find(searchterm);
      const textArr = searchterm.split(" ");
      entries.map((data) => {
        var storeDoc = storeDocs[data.item];
        // 匹配度=文本包含的关键字数量
        storeDoc.relative_score = 0;
        textArr.map((value) => {
          storeDoc.text.includes(value) && (storeDoc.relative_score += 1);
        })
      });
      // 按照匹配度进行排序,匹配度高的在前
      entries.sort((a,b)=>{
        return storeDocs[b.item].relative_score - storeDocs[a.item].relative_score;
      })
      return entries.map((data) => {
        return {
          doc: storeDocs[data.item],
          ref: data.item,
          score: data.score,
        };
      });
    },
  };
};
