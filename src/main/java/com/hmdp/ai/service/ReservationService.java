package com.hmdp.ai.service;

import com.hmdp.ai.entity.Reservation;
import com.hmdp.ai.mapper.ReservationMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ReservationService {

    @Resource
    private ReservationMapper reservationMapper;

    public void insert(Reservation reservation) {
        reservationMapper.insertReservation(reservation);
    }

    public List<Reservation> findByPhone(String phone) {
        return reservationMapper.findByPhone(phone);
    }
}
