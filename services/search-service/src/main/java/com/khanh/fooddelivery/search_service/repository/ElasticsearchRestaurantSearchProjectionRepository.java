package com.khanh.fooddelivery.search_service.repository;

import com.khanh.fooddelivery.search_service.config.SearchProperties;
import com.khanh.fooddelivery.search_service.document.RestaurantBranchSearchProjection;
import com.khanh.fooddelivery.search_service.document.RestaurantSearchProjection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Repository
public class ElasticsearchRestaurantSearchProjectionRepository implements RestaurantSearchProjectionRepository {
    private static final String RESTAURANT_SCRIPT = """
            if (ctx._source.restaurantAggregateVersion == null || params.aggregateVersion > ctx._source.restaurantAggregateVersion) {
              ctx._source.restaurantId=params.restaurantId; ctx._source.name=params.name; ctx._source.description=params.description;
              ctx._source.status=params.status; ctx._source.restaurantCode=params.restaurantCode; ctx._source.logoUrl=params.logoUrl;
              ctx._source.coverImageUrl=params.coverImageUrl; ctx._source.restaurantAggregateVersion=params.aggregateVersion;
              ctx._source.restaurantLastEventId=params.lastEventId; if(ctx._source.branches==null){ctx._source.branches=[];}
            } else { ctx.op='noop'; }
            """;
    private static final String BRANCH_SCRIPT = """
            if(ctx._source.restaurantId==null){ctx._source.restaurantId=params.restaurantId;} if(ctx._source.branches==null){ctx._source.branches=[];}
            int found=-1; for(int i=0;i<ctx._source.branches.size();i++){if(ctx._source.branches.get(i).branchId==params.branch.branchId){found=i;break;}}
            if(found==-1){ctx._source.branches.add(params.branch);} else {def current=ctx._source.branches.get(found); if(current.aggregateVersion==null || params.branch.aggregateVersion>current.aggregateVersion){ctx._source.branches.set(found,params.branch);}else{ctx.op='noop';}}
            """;
    private final RestClient client; private final SearchProperties properties;
    public ElasticsearchRestaurantSearchProjectionRepository(@Qualifier("searchElasticsearchRestClient") RestClient client, SearchProperties properties) { this.client=client; this.properties=properties; }
    public void createIndexIfAbsent() { try {client.get().uri("/{index}",properties.getRestaurantIndexName()).retrieve().toBodilessEntity();} catch(RestClientResponseException e){if(e.getStatusCode().value()!=404) throw e; create();} }
    public void recreateIndex() { try {client.delete().uri("/{index}",properties.getRestaurantIndexName()).retrieve().toBodilessEntity();} catch(RestClientResponseException e){if(e.getStatusCode().value()!=404)throw e;} create(); }
    public void applyRestaurant(RestaurantSearchProjection p) { Map<String,Object> params=new LinkedHashMap<>(); params.put("restaurantId",p.restaurantId().toString());params.put("name",p.name());params.put("description",p.description());params.put("status",p.status());params.put("restaurantCode",p.restaurantCode());params.put("logoUrl",p.logoUrl());params.put("coverImageUrl",p.coverImageUrl());params.put("aggregateVersion",p.aggregateVersion());params.put("lastEventId",p.lastEventId().toString());update(p.restaurantId(),RESTAURANT_SCRIPT,params,Map.of("restaurantId",p.restaurantId().toString(),"branches",List.of())); }
    public void applyBranch(RestaurantBranchSearchProjection p, UUID restaurantId) { Map<String,Object> branch=new LinkedHashMap<>();branch.put("branchId",p.branchId().toString());branch.put("name",p.name());branch.put("status",p.status());branch.put("addressLine",p.addressLine());branch.put("ward",p.ward());branch.put("district",p.district());branch.put("city",p.city());branch.put("latitude",p.latitude());branch.put("longitude",p.longitude());branch.put("acceptingOrders",p.acceptingOrders());branch.put("aggregateVersion",p.aggregateVersion());branch.put("lastEventId",p.lastEventId().toString());update(restaurantId,BRANCH_SCRIPT,Map.of("restaurantId",restaurantId.toString(),"branch",branch),Map.of("restaurantId",restaurantId.toString(),"branches",List.of())); }
    private void update(UUID id,String script,Map<String,Object>params,Map<String,Object>upsert){client.post().uri("/{index}/_update/{id}",properties.getRestaurantIndexName(),id).body(Map.of("scripted_upsert",true,"script",Map.of("lang","painless","source",script,"params",params),"upsert",upsert)).retrieve().toBodilessEntity();}
    private void create(){client.put().uri("/{index}",properties.getRestaurantIndexName()).body(Map.of("settings",Map.of("analysis",Map.of("analyzer",Map.of("folded_text",Map.of("type","custom","tokenizer","standard","filter",List.of("lowercase","asciifolding"))))),"mappings",Map.of("dynamic","strict","properties",mappings()))).retrieve().toBodilessEntity();}
    private Map<String,Object> mappings(){Map<String,Object> m=new LinkedHashMap<>();m.put("restaurantId",Map.of("type","keyword"));m.put("name",text());m.put("description",Map.of("type","text","analyzer","folded_text"));m.put("status",Map.of("type","keyword"));m.put("restaurantCode",Map.of("type","keyword"));m.put("logoUrl",Map.of("type","keyword","index",false));m.put("coverImageUrl",Map.of("type","keyword","index",false));m.put("restaurantAggregateVersion",Map.of("type","long"));m.put("restaurantLastEventId",Map.of("type","keyword"));m.put("branches",Map.of("type","nested","properties",branchMappings()));return m;}
    private Map<String,Object> text(){return Map.of("type","text","analyzer","folded_text","fields",Map.of("keyword",Map.of("type","keyword")));}
    private Map<String,Object> branchMappings(){Map<String,Object> m=new LinkedHashMap<>();m.put("branchId",Map.of("type","keyword"));m.put("name",text());m.put("status",Map.of("type","keyword"));m.put("addressLine",Map.of("type","text","analyzer","folded_text"));m.put("ward",Map.of("type","keyword"));m.put("district",Map.of("type","keyword"));m.put("city",Map.of("type","keyword"));m.put("latitude",Map.of("type","scaled_float","scaling_factor",1000000));m.put("longitude",Map.of("type","scaled_float","scaling_factor",1000000));m.put("acceptingOrders",Map.of("type","boolean"));m.put("aggregateVersion",Map.of("type","long"));m.put("lastEventId",Map.of("type","keyword"));return m;}
}
