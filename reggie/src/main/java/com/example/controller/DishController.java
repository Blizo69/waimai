package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.R;
import com.example.dto.DishDto;
import com.example.entity.Category;
import com.example.entity.Dish;
import com.example.entity.DishFlavor;
import com.example.entity.SetmealDish;
import com.example.service.*;
import com.example.utils.deletePicture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Value("${reggie.path}")
    private String basePath;


    @PostMapping()
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);
        return R.success("添加成功！");
    }

    /**
     * 菜品信息的分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page<DishDto>> page(int page, int pageSize, String name){
        //构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        //添加条件
        queryWrapper.like(name != null,Dish::getName,name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //分页查询
        dishService.page(pageInfo, queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();//分类id
            Category category = categoryService.getById(categoryId);
            String categoryName = category.getName();
            dishDto.setCategoryName(categoryName);

            return dishDto;

        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id获取信息
     * @param dishId
     * @return
     */
    @GetMapping("/{dishId}")
    public R<DishDto> get(@PathVariable Long dishId){
        DishDto dishDto = dishService.getByIdWithFlavor(dishId);
        return R.success(dishDto);
    }

    /**
     * 更新信息
     * @param dishDto
     * @return
     */
    @PutMapping()
    public R<String> update(@RequestBody DishDto dishDto){
        dishService.updateWithFlavor(dishDto);
        return R.success("修改成功！");
    }

    /**
     *根据分类id查找该分类下的菜品信息
     * @param dish
     * @return
     */
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Dish::getCategoryId,dish.getCategoryId());
//        queryWrapper.eq(Dish::getStatus,1);
//        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//        List<Dish> list = dishService.list(queryWrapper);
//        return R.success(list);
//    }

    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        //查找对应的菜品数据
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //按照categoryId查找菜品
        queryWrapper.eq(Dish::getCategoryId,dish.getCategoryId());
        //查找状态为1，即代售的菜品
        queryWrapper.eq(Dish::getStatus,1);
        //菜品的排序规则
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> dishList = dishService.list(queryWrapper);
        List<DishDto> dishDtoList = dishList.stream().map((item) -> {
            //根据菜品id即dishId查找口味信息
            //创建条件构造器对象
            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,item.getId());
            //排序条件
            dishFlavorLambdaQueryWrapper.orderByDesc(DishFlavor::getUpdateTime);
            List<DishFlavor> dishFlavorList = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
            //查找菜品分类名称
            Category category = categoryService.getById(item.getCategoryId());
            //创建dishDto对象封装数据
            DishDto dishDto = new DishDto();
            //封装dish数据
            BeanUtils.copyProperties(item,dishDto);
            //封装dishFlavors数据
            dishDto.setFlavors(dishFlavorList);
            //封装菜品分类名称
            dishDto.setCategoryName(category.getName());
            return dishDto;
        }).collect(Collectors.toList());

        return R.success(dishDtoList);
    }

    @PostMapping("/status/0")
    public R<String> updateStatusTo0ById(@RequestParam List<Long> ids){
        //循环修改list集合中id对应的菜品的状态
        ids.stream().map((item) -> {
            //创建dish对象存储数据
            Dish dish = new Dish();
            dish.setId(item);
            dish.setStatus(0);

            //修改id对应的菜品的状态
            dishService.updateById(dish);
            return item;
        }).collect(Collectors.toList());

        return R.success("修改成功！");
    }

    @PostMapping("/status/1")
    public R<String> updateStatusTo1ById(@RequestParam List<Long> ids){

        //循环修改list集合中id对应的菜品的状态
        ids.stream().map((item) -> {
            //创建dish对象存储数据
            Dish dish = new Dish();
            dish.setId(item);
            dish.setStatus(1);

            //修改id对应的菜品的状态
            dishService.updateById(dish);
            return item;
        }).collect(Collectors.toList());

        return R.success("修改成功！");
    }

    @DeleteMapping()
    public R<String> deleteByIds(@RequestParam List<Long> ids){

        //删除之前查看是否绑定了套餐
        StringBuffer misinformation = new StringBuffer();
        ids.stream().map((item) -> {
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SetmealDish::getDishId,item);
            List<SetmealDish> list = setmealDishService.list(queryWrapper);
            if(list.size() != 0)
                misinformation.insert(misinformation.length(),list.get(0).getName()+",");
            return item;
        }).collect(Collectors.toList());

        if(misinformation.length() != 0){
            return R.error(misinformation+"绑定了套餐，不能直接删除");
        }

        //删除菜品相关的口味信息和图片文件
        ids.stream().map((item) -> {
            //创建条件构造器对象
            LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DishFlavor::getDishId,item);

            //删除口味信息
            dishFlavorService.remove(queryWrapper);

            //删除图片信息
            Dish dish = dishService.getById(item);
            try {
                log.info("开始删除图片");
                deletePicture.deleteByName(basePath+dish.getImage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return item;
        }).collect(Collectors.toList());

        //删除菜品信息
        dishService.removeByIds(ids);

        return R.success("删除成功！");
    }

}
