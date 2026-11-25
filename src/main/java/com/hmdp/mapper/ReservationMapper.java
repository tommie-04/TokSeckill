package com.hmdp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.entity.Reservation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    @Insert("insert into reservation(name, phone, communication_time, shop_name) values(#{name}, #{phone}, #{communicationTime}, #{shopName})")
    void insertReservation(Reservation reservation);

    @Select("select * from reservation where phone = #{phone}")
    List<Reservation> findByPhone(String phone);
}
