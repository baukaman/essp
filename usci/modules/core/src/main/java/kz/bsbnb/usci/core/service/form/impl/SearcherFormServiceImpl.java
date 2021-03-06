package kz.bsbnb.usci.core.service.form.impl;

import kz.bsbnb.usci.core.service.form.ISearcherFormService;
import kz.bsbnb.usci.core.service.form.pool.ISearcherFormPool;
import kz.bsbnb.usci.core.service.form.searcher.ISearcherForm;
import kz.bsbnb.usci.eav.model.meta.IMetaClass;
import kz.bsbnb.usci.eav.model.meta.impl.MetaClass;
import kz.bsbnb.usci.eav.model.searchForm.ISearchResult;
import kz.bsbnb.usci.eav.util.Errors;
import kz.bsbnb.usci.eav.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearcherFormServiceImpl implements ISearcherFormService {

    @Autowired
    ISearcherFormPool searcherFormPool;

    public List<String[]> getMetaClasses(long userId){
        List<String[]> ret = new LinkedList<>();

        for(ISearcherForm sf : searcherFormPool.getSearcherForms()) {
            List<Pair> metas = sf.getMetaClasses(userId);
            for(Pair p : metas) {
                ret.add(new String[]{sf.getClass().getName(), p.getName(), p.getTitle()});
            }
        }

        return ret;
    }

    private ISearcherForm getSearchForm(String searchClassName){
        for(ISearcherForm sf : searcherFormPool.getSearcherForms()) {
            if(sf.getClass().getName().equals(searchClassName))
                return sf;
        }

        throw new RuntimeException(Errors.compose(Errors.E229));
    }

    @Override
    public String getDom(Long userId, String search, IMetaClass metaClass, String prefix) {
        ISearcherForm sf = searcherFormPool.getSearchForm(search);
        return sf.getDom(userId, metaClass, prefix);
    }

    @Override
    public ISearchResult search(String searchClassName, HashMap<String, String> parameters, MetaClass metaClass,String prefix, long creditorId) {
        ISearcherForm sf = getSearchForm(searchClassName);
        return sf.search(parameters, metaClass, prefix, creditorId);
    }
}
