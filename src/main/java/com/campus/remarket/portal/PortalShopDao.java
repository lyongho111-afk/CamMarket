package com.campus.remarket.portal;
import com.campus.remarket.dto.ShopDto;
import com.campus.remarket.dto.MannerRatingDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface PortalShopDao {
    ShopDto forOwner(@Param("ownerId")Long ownerId);
    ShopDto lock(@Param("id")Long id);
    int create(ShopDto shop);
    int updateInfo(ShopDto shop);
    int updateOperations(ShopDto shop);
    int updateStatus(ShopDto shop);
    int approval(@Param("id")Long id,@Param("approved")boolean approved);
    int assign(@Param("id")Long id,@Param("ownerId")Long ownerId,@Param("name")String name,@Param("businessNo")String businessNo);
    int location(@Param("id") Long id,@Param("lat") java.math.BigDecimal lat,@Param("lng") java.math.BigDecimal lng,@Param("address") String address);
    List<MannerRatingDto> ratings();
    List<com.campus.remarket.dto.TradeReviewDto> reviews();
}
