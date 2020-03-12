package com.epoint.kill.server.service;

import com.epoint.kill.model.dto.KillSuccessUserInfo;
import com.epoint.kill.model.entity.Item;
import com.epoint.kill.model.entity.ItemKill;
import com.epoint.kill.model.entity.ItemKillSuccess;

import java.util.List;

//获取秒杀商品列表
public interface ItemService {
    //获取秒杀商品
    List<ItemKill> getItemKills();

    boolean execKill(String id,Integer userId);

    //获取秒杀商品详情
    Item getItemById(String id);

    KillSuccessUserInfo getItemByCode(String code);

    boolean execKillV2(String id,Integer userId);

    boolean execKillV3(String id,Integer userId);

    boolean execKillV4(String id,Integer userId);

    boolean execKillV5(String id,Integer userId);
}
