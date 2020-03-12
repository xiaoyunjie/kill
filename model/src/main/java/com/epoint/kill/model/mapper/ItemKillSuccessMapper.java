package com.epoint.kill.model.mapper;

import com.epoint.kill.model.dto.KillSuccessUserInfo;
import com.epoint.kill.model.entity.ItemKillSuccess;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ItemKillSuccessMapper {
    int deleteByPrimaryKey(String code);

    int insert(ItemKillSuccess record);

    int insertSelective(ItemKillSuccess record);

    ItemKillSuccess selectByPrimaryKey(String code);

    int updateByPrimaryKeySelective(ItemKillSuccess record);

    int updateByPrimaryKey(ItemKillSuccess record);

      //param 与xml 文件中的变量相对应
    int countByKillUserId(@Param("killId") Integer killId, @Param("userId") Integer userId);

    //根据订单编号获取详细信息
    KillSuccessUserInfo selectByCode(@Param("code") String code);

    int expireOrder(@Param("code") String code);

    List<ItemKillSuccess> selectExpireOrders();
}