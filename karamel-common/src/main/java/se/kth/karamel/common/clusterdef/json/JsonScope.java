/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.karamel.common.clusterdef.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import se.kth.karamel.common.exception.KaramelException;
import se.kth.karamel.common.clusterdef.Scope;
import se.kth.karamel.common.cookbookmeta.Attribute;
import se.kth.karamel.common.cookbookmeta.KaramelizedCookbook;
import se.kth.karamel.common.clusterdef.yaml.YamlCluster;
import se.kth.karamel.common.clusterdef.yaml.YamlScope;
import se.kth.karamel.common.exception.ValidationException;
import se.kth.karamel.common.cookbookmeta.CookbookCache;

/**
 *
 * @author kamal
 */
public class JsonScope extends Scope {

  private final List<JsonCookbook> cookbooks = new ArrayList<>();
  public static CookbookCache CACHE;
  
  public JsonScope() {
  }

  public JsonScope(YamlCluster cluster, YamlScope scope) throws KaramelException {
    super(scope);
    Map<String, Object> usedAttrs = cluster.flattenAttrs();
    List<KaramelizedCookbook> allCookbooks = CACHE.loadAllKaramelizedCookbooks(cluster);
    //filtering invalid(not defined in metadata.rb) attributes from yaml model
    for (KaramelizedCookbook kcb : allCookbooks) {
      List<Attribute> allValidAttrs = kcb.getMetadataRb().getAttributes();
      Map<String, Object> validUsedAttrs = new HashMap<>();
      for (Attribute att : allValidAttrs) {
        if (usedAttrs.containsKey(att.getName())) {
          validUsedAttrs.put(att.getName(), usedAttrs.get(att.getName()));
        }
      }
      JsonCookbook jck = new JsonCookbook(kcb.getUrls().id, kcb.getMetadataRb().getName(), 
          kcb.getMetadataRb().getName(), validUsedAttrs);
      cookbooks.add(jck);
    }
    
    Map<String, Object> invalidAttrs = new HashMap<>();
    invalidAttrs.putAll(usedAttrs);
    for (JsonCookbook jc : cookbooks) {
      Map<String, Object> attrs1 = jc.getAttrs();
      for (Map.Entry<String, Object> entry : attrs1.entrySet()) {
        String key = entry.getKey();
        if (invalidAttrs.containsKey(key)) {
          invalidAttrs.remove(key);
        }
      }
    }

    if (!invalidAttrs.isEmpty()) {
      throw new KaramelException(String.format("Invalid attributes, all used attributes must be defined in metadata.rb "
          + "files: %s", invalidAttrs.keySet().toString()));
    }

  }

  public List<JsonCookbook> getCookbooks() {
    return cookbooks;
  }

  public void setCookbooks(List<JsonCookbook> cookbooks) {
    this.cookbooks.addAll(cookbooks);
  }

  @Override
  public String getAttr(String key) {
    throw new UnsupportedOperationException("Not supported yet."); 
  }

  @Override
  public void validate() throws ValidationException {
    super.validate();
  }

}
