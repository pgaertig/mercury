package pl.amitec.mercury.integrators.redbay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.clients.bitbee.types.Category;
import pl.amitec.mercury.clients.bitbee.types.TranslatedName;
import pl.redbay.ws.client.types.RbArrayOfCategories;
import pl.redbay.ws.client.types.RbCategoryTranslation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Redbay category tree to Bitbee category paths.
 */
//TODO test
public class CategoryMapper {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryMapper.class);
    private Map<String, List<Category>> categoryPaths = new HashMap<>();

    public CategoryMapper(RbArrayOfCategories topCategories) {
        mapRbCategoryTreeToBBCategoryPaths(List.of(), topCategories);
    }

    public List<Category> getBitbeeCategoryPath(RbArrayOfCategories productCategories) {
        if(productCategories == null || productCategories.getItems().isEmpty()) {
            return List.of();
        } else {
            List<Category> path = categoryPaths.get(productCategories.getItems().getLast().getId());
            if(path == null) {
                LOG.warn("No category path found for Redbay category id: {}, productCategories: {}",
                        productCategories.getItems().getLast().getId(), productCategories);
                return List.of();
            }
            return path;
        }
    }

    private void mapRbCategoryTreeToBBCategoryPaths(
            List<Category> prefixPath,
            RbArrayOfCategories arrayOfCategories) {

        arrayOfCategories.getItems().forEach(rbCategory -> {
            Category bbCategory = mapCategory(rbCategory);
            List<Category> path = concat(prefixPath, bbCategory);
            categoryPaths.put(rbCategory.getId(), path);
            if(rbCategory.getChilds() != null && !rbCategory.getChilds().getItems().isEmpty()) {
                mapRbCategoryTreeToBBCategoryPaths(path, rbCategory.getChilds());
            }
        });
    }

    private Category mapCategory(pl.redbay.ws.client.types.RbCategory rbCategory) {
        return Category.builder()
                .sourceId(rbCategory.getId())
                //TODO i18n
                .name(translate(rbCategory.getTranslations().getItems()))
                .build();
    }

    private TranslatedName translate(List<RbCategoryTranslation> rbCategoryTranslations) {
        TranslatedName bbTrans = new TranslatedName();
        rbCategoryTranslations.forEach(translations ->
                bbTrans.add(translations.getLanguage().getIso(), translations.getValue())
        );
        return bbTrans;
    }

    private List<Category> concat(List<Category> prefix, Category category) {
        ArrayList<Category> updatedList = new ArrayList<>(prefix);
        updatedList.add(category);
        return List.copyOf(updatedList);
    }
}
