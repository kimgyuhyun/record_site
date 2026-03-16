package com.recordsite.backend.controller;

import com.recordsite.backend.dto.ItemDto;
import com.recordsite.backend.service.ItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @Test
    void getItemList() throws Exception {
        ItemDto item1 = new ItemDto();
        ItemDto item2 = new ItemDto();
        item1.setItemName("롱소드");
        item2.setItemName("도란검");

        when(itemService.findAllItemList()).thenReturn(List.of(item1, item2));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemName").value("롱소드"))
                .andExpect(jsonPath("$[1].itemName").value("도란검"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getItemList_Empty_Test() throws Exception {

        when(itemService.findAllItemList())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

}