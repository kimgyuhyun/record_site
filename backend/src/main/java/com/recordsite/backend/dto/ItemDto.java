package com.recordsite.backend.dto;

import com.recordsite.backend.entity.Item;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ItemDto {

    private String itemKey;

    private String itemName;

    private String description;

    private String image;

    private int goldBase;

    private int goldSell;


    public static ItemDto from(Item item) {
        ItemDto dto = new ItemDto();
        dto.setItemKey(item.getItemKey());
        dto.setItemName(item.getItemName());
        dto.setDescription(item.getDescription());
        dto.setImage(item.getImage());
        dto.setGoldBase(item.getGoldBase());
        dto.setGoldSell(item.getGoldSell());
        return dto;
    }


}
