package com.tokenflash.ai.service;

import com.tokenflash.ai.entity.Reservation;
import com.tokenflash.ai.mapper.ReservationMapper;
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
