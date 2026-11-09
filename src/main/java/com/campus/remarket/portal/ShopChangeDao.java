package com.campus.remarket.portal;
import com.campus.remarket.dto.ShopChangeDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface ShopChangeDao {
    int insert(ShopChangeDto dto);
    List<ShopChangeDto> forOwner(@Param("ownerId")Long ownerId);
}
