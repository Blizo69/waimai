package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dto.DishDto;
import com.example.entity.Dish;
import com.example.entity.DishFlavor;
import com.example.mapper.DishMapper;
import com.example.service.DishFlavorService;
import com.example.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 新增菜品，同时保存对用的口味数据
     * @param dishDto
     */
    @Override
//    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到菜品表dish
        this.save(dishDto);

        Long dishid = dishDto.getId();

        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
//        for (DishFlavor item : flavors) {
//            item.setDishId(dishid);
//        }

        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishid);
            return item;
        }).collect(Collectors.toList());

        //保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(flavors);

    }

    /**
     * 根据dishid查找dish信息和dish的flavors
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id){
        //根据id查dish内容
        Dish dish = this.getById(id);

        //根据dishId查dishFlavor内容
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,id);
        List<DishFlavor> list = dishFlavorService.list(queryWrapper);

        //将dish和dishFlavor的内容赋给dishDto
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);
        dishDto.setFlavors(list);

        return dishDto;
    }

    /**
     * 更新菜品信息
     * @param dishDto
     */
    @Override
    public void updateWithFlavor(DishDto dishDto){
        //将dishDto的dish的数据复制出来
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDto,dish);
        this.updateById(dish);

        //获取flavors的数据
        List<DishFlavor> dishFlavors = dishDto.getFlavors();
        log.info(dishFlavors.toString());

        //先删除原本的flavors的数据
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());
        dishFlavorService.remove(queryWrapper);

        //给flavors添加dishId
        dishFlavors = dishFlavors.stream().map((item) -> {
            item.setDishId(dish.getId());
            return item;
        }).collect(Collectors.toList());

        //将新的flavors数据添加到数据库中的dishFlavors表中
        dishFlavorService.saveBatch(dishFlavors);
    }

}
