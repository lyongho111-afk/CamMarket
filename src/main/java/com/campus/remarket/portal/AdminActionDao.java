package com.campus.remarket.portal;
import com.campus.remarket.dto.AdminActionDto;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
@Mapper
public interface AdminActionDao {
    int insert(AdminActionDto action);
    List<AdminActionDto> recent();
    AdminActionDto latestPublication(Long shopId);
}
