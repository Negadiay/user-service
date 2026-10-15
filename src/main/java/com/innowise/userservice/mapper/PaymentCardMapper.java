package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.PaymentCardRequestDto;
import com.innowise.userservice.dto.PaymentCardResponseDto;
import com.innowise.userservice.entity.PaymentCard;
import java.util.List;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentCardMapper {

    @Mapping(target = "userId", source = "user.id")
    PaymentCardResponseDto toDto(PaymentCard card);

    List<PaymentCardResponseDto> toDtoList(List<PaymentCard> cards);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentCard toEntity(PaymentCardRequestDto dto);

    @InheritConfiguration(name = "toEntity")
    void updateEntity(PaymentCardRequestDto dto, @MappingTarget PaymentCard card);
}