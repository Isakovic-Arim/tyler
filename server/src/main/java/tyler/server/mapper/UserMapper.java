package tyler.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import tyler.server.dto.user.UserProfileDto;
import tyler.server.entity.User;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT
)
public abstract class UserMapper {
    @Mapping(target = "username", source = "username")
    @Mapping(target = "currentXp", source = "currentXp")
    @Mapping(target = "dailyQuota", source = "dailyXpQuota")
    @Mapping(target = "currentStreak", source = "currentStreak")
    @Mapping(target = "daysOff", source = "daysOff")
    @Mapping(target = "daysOffPerWeek", source = "daysOffPerWeek")
    public abstract UserProfileDto toUserProfileDto(User user);
}