package com.roncoo.eshop.rabbitmq;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.roncoo.eshop.service.EshopProductService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

@Component
@RabbitListener(queues = "data-change-queue")
public class DataChangeQueueReceiver {

    @Autowired
    private EshopProductService eshopProductService;
    @Autowired
    private JedisPool jedisPool;
    @Autowired
    private RabbitMQSender rabbitMQSender;

    private Set<String> dimDataChangeMessageSet
            = Collections.synchronizedSet(new HashSet<String>());

    private List<JSONObject> brandDataChangeMessageList = new ArrayList<JSONObject>();

    public DataChangeQueueReceiver(){
        new SendThread().start();
    }

    @RabbitHandler
    public void process(String message){
        JSONObject jsonObject = JSONObject.parseObject(message);

        String dataType = jsonObject.getString("data_type");
        if("brand".equals(dataType)) {
            processBrandDataChangeMessage(jsonObject);
        } else if("category".equals(dataType)) {
            processCategoryDataChangeMessage(jsonObject);
        } else if("product_intro".equals(dataType)) {
            processProductIntroDataChangeMessage(jsonObject);
        } else if("product_property".equals(dataType)) {
            processProductPropertyDataChangeMessage(jsonObject);
        } else if("product".equals(dataType)) {
            processProductDataChangeMessage(jsonObject);
        } else if("product_specification".equals(dataType)) {
            processProductSpecificationDataChangeMessage(jsonObject);
        }
    }

    private void processBrandDataChangeMessage(JSONObject messageJSONObject) {
        Long id = messageJSONObject.getLong("id");
        String eventType = messageJSONObject.getString("event_type");

        if("add".equals(eventType) || "update".equals(eventType)) {
            brandDataChangeMessageList.add(messageJSONObject);

            System.out.println("???????????????????????????list?????????list.size = " + brandDataChangeMessageList.size());

            if(brandDataChangeMessageList.size() >= 2){
                System.out.println("???????????????????????????list??? >= 2???");
                String ids = "";
                for(int i = 0; i < brandDataChangeMessageList.size(); i++){
                    ids += brandDataChangeMessageList.get(i).getLong("id");
                    if(i < brandDataChangeMessageList.size() - 1)
                     ids += ",";
                }
                System.out.println("???????????????id????????? ids = " + ids);
                JSONArray brandJSONArray = JSONArray.parseArray(eshopProductService.findBrandByIds(ids));
                System.out.println("???????????????????????????????????? jsonArray = " + brandJSONArray.toJSONString());
                for(int i = 0; i < brandJSONArray.size(); i++){
                    JSONObject dataJSONObject = brandJSONArray.getJSONObject(i);
                    Jedis jedis = jedisPool.getResource();
                    jedis.set("brand_" + dataJSONObject.getLong("id"), dataJSONObject.toJSONString());

                    System.out.println("?????????????????????redis??? brandId = " + dataJSONObject.getLong("id"));

                    dimDataChangeMessageSet.add("{\"dim_type\": \"brand\", \"id\": " + dataJSONObject.getLong("id") + "}");
                }

                brandDataChangeMessageList.clear();
            }

        } else if ("delete".equals(eventType)) {
            Jedis jedis = jedisPool.getResource();
            jedis.del("brand_" + id);
        }

        //rabbitMQSender.send("aggr-data-change-queue", "{\"dim_type\": \"brand\", \"id\": " + id + "}");
        dimDataChangeMessageSet.add("{\"dim_type\": \"brand\", \"id\": " + id + "}");
    }

    private void processCategoryDataChangeMessage(JSONObject messageJSONObject) {
        Long id = messageJSONObject.getLong("id");
        String eventType = messageJSONObject.getString("event_type");

        if("add".equals(eventType) || "update".equals(eventType)) {
            JSONObject dataJSONObject = JSONObject.parseObject(eshopProductService.findCategoryById(id));
            Jedis jedis = jedisPool.getResource();
            jedis.set("category_" + dataJSONObject.getLong("id"), dataJSONObject.toJSONString());
        } else if ("delete".equals(eventType)) {
            Jedis jedis = jedisPool.getResource();
            jedis.del("category_" + id);
        }

        //rabbitMQSender.send("aggr-data-change-queue", "{\"dim_type\": \"category\", \"id\": " + id + "}");
        dimDataChangeMessageSet.add("{\"dim_type\": \"category\", \"id\": " + id + "}");

    }

    private void processProductIntroDataChangeMessage(JSONObject messageJSONObject) {
        Long id = messageJSONObject.getLong("id");
        Long productId = messageJSONObject.getLong("product_id");
        String eventType = messageJSONObject.getString("event_type");

        if("add".equals(eventType) || "update".equals(eventType)) {
            JSONObject dataJSONObject = JSONObject.parseObject(eshopProductService.findProductIntroById(id));
            Jedis jedis = jedisPool.getResource();
            jedis.set("product_intro_" + productId, dataJSONObject.toJSONString());
        } else if ("delete".equals(eventType)) {
            Jedis jedis = jedisPool.getResource();
            jedis.del("product_intro_" + productId);
        }

        //rabbitMQSender.send("aggr-data-change-queue", "{\"dim_type\": \"product_intro\", \"id\": " + productId + "}");
        dimDataChangeMessageSet.add("{\"dim_type\": \"product_intro\", \"id\": " + productId + "}");
    }

    private void processProductDataChangeMessage(JSONObject messageJSONObject) {
        Long id = messageJSONObject.getLong("id");
        String eventType = messageJSONObject.getString("event_type");

        if("add".equals(eventType) || "update".equals(eventType)) {
            JSONObject dataJSONObject = JSONObject.parseObject(eshopProductService.findProductById(id));
            Jedis jedis = jedisPool.getResource();
            jedis.set("product_" + id, dataJSONObject.toJSONString());
        } else if ("delete".equals(eventType)) {
            Jedis jedis = jedisPool.getResource();
            jedis.del("product_" + id);
        }

        //rabbitMQSender.send("aggr-data-change-queue", "{\"dim_type\": \"product\", \"id\": " + id + "}");
        dimDataChangeMessageSet.add("{\"dim_type\": \"product\", \"id\": " + id + "}");
    }

    private void processProductPropertyDataChangeMessage(JSONObject messageJSONObject) {
        Long id = messageJSONObject.getLong("id");
        Long productId = messageJSONObject.getLong("product_id");
        String eventType = messageJSONObject.getString("event_type");

        if("add".equals(eventType) || "update".equals(eventType)) {
            JSONObject dataJSONObject = JSONObject.parseObject(eshopProductService.findProductPropertyById(id));
            Jedis jedis = jedisPool.getResource();
            jedis.set("product_property_" + productId, dataJSONObject.toJSONString());
        } else if ("delete".equals(eventType)) {
            Jedis jedis = jedisPool.getResource();
            jedis.del("product_property_" + productId);
        }

        //rabbitMQSender.send("aggr-data-change-queue", "{\"dim_type\": \"product\", \"id\": " + productId + "}");
        dimDataChangeMessageSet.add("{\"dim_type\": \"product\", \"id\": " + productId + "}");
    }

    private void processProductSpecificationDataChangeMessage(JSONObject messageJSONObject) {
        Long id = messageJSONObject.getLong("id");
        Long productId = messageJSONObject.getLong("product_id");
        String eventType = messageJSONObject.getString("event_type");

        if("add".equals(eventType) || "update".equals(eventType)) {
            JSONObject dataJSONObject = JSONObject.parseObject(eshopProductService.findProductSpecificationById(id));
            Jedis jedis = jedisPool.getResource();
            jedis.set("product_specification_" + productId, dataJSONObject.toJSONString());
        } else if ("delete".equals(eventType)) {
            Jedis jedis = jedisPool.getResource();
            jedis.del("product_specification_" + productId);
        }

        //rabbitMQSender.send("aggr-data-change-queue", "{\"dim_type\": \"product\", \"id\": " + productId + "}");
        dimDataChangeMessageSet.add("{\"dim_type\": \"product\", \"id\": " + productId + "}");
    }

    private class SendThread extends Thread{
        public void run(){
            while(true){
                if(!dimDataChangeMessageSet.isEmpty()){
                    for(String message : dimDataChangeMessageSet)
                        rabbitMQSender.send("aggr-data-change-queue", message);
                    dimDataChangeMessageSet.clear();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
