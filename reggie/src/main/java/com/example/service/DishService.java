package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dto.DishDto;
import com.example.entity.Dish;

public interface DishService extends IService<Dish> {

    //新增菜品，同时插入菜品对应的口味数据，需要操作两张表
    public void saveWithFlavor(DishDto dishDto);

    //根据dishId查找dish信息和dishFlavor信息并封装到dishDto中
    public DishDto getByIdWithFlavor(Long id);

    //更新菜品信息
    void updateWithFlavor(DishDto dishDto);
}
