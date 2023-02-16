package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.R;
import com.example.dto.DishDto;
import com.example.dto.SetmealDto;
import com.example.entity.*;
import com.example.service.*;
import com.example.utils.deletePicture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/setmeal")
public class SetmealController {


    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Value("${reggie.path}")
    private String basePath;

    @PostMapping()
    @Transactional
    @CacheEvict(value = "setmealCache" ,allEntries = true)
    public R<String> save(@RequestBody SetmealDto setmealDto){
//        log.info(setmealDto.toString());
        //获取setmealDto中的setmeal信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDto,setmeal);

        //保存setmeal信息
        setmealService.save(setmeal);

        //获取setmeal中对应菜品dish的信息保存到列表list中
        List<SetmealDish> list = setmealDto.getSetmealDishes();

        //对列表list进行处理，给每一个dish赋上setmealId
        list.stream().map((item) -> {
            item.setSetmealId(setmeal.getId());
            return item;
        }).collect(Collectors.toList());

        //将dish信息的list保存到setmealDish表中
        setmealDishService.saveBatch(list);

        return R.success("保存成功！");
    }

    @GetMapping("/page")
    public R<Page<SetmealDto>> get( int page, int pageSize,String name){
        //构造分页构造器
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);

        //条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null,Setmeal::getName,name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        //分页查询
        setmealService.page(pageInfo,queryWrapper);

        //对象拷贝
        Page<SetmealDto> pageDtoInfo = new Page<>();
        BeanUtils.copyProperties(pageInfo,pageDtoInfo,"records");

        //对象处理，给pageDtoInfo查找categoryName
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> list = records.stream().map((item) -> {
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item,setmealDto);
            setmealDto.setCategoryName(category.getName());
            return setmealDto;
        }).collect(Collectors.toList());

        //拷贝添加了categoryName的setmeal集合到page中
        pageDtoInfo.setRecords(list);

        return R.success(pageDtoInfo);
    }

    /**
     * 根据setmealid查找setmeal和setmealdish信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id){

        SetmealDto setmealDto = new SetmealDto();

        //根据id查找setmeal信息
        Setmeal setmeal = setmealService.getById(id);
        BeanUtils.copyProperties(setmeal,setmealDto);

        //根据id查找setmealDish信息
        //创建条件构造器对象
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,id);
        //查找setmealDish
        List<SetmealDish> list = setmealDishService.list(queryWrapper);

        setmealDto.setSetmealDishes(list);

        return R.success(setmealDto);
    }

    @PutMapping()
    @CacheEvict(value = "setmealCache" ,allEntries = true)
    public R<String> update(@RequestBody SetmealDto setmealDto){

        //获取setmealDto中setmeal的信息
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDto,setmeal);

        //更新setmeal数据
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getId,setmeal.getId());
        setmealService.update(setmeal,queryWrapper);

        //更新菜品数据
        //先删除原有菜品数据
        LambdaQueryWrapper<SetmealDish> queryWrapperDishes = new LambdaQueryWrapper<>();
        queryWrapperDishes.eq(SetmealDish::getSetmealId,setmeal.getId());
        setmealDishService.remove(queryWrapperDishes);

        //将新菜品信息添加到setmealdish表中
        //给新菜品赋值setmealId
        List<SetmealDish> listDishes = setmealDto.getSetmealDishes();
        listDishes = listDishes.stream().map((item) -> {
            item.setSetmealId(setmeal.getId());
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(listDishes);

        return R.success("修改成功！");
    }

    /**
     * 删除套餐操作
     * @param ids
     * @return
     */
    @DeleteMapping()
    @CacheEvict(value = "setmealCache" ,allEntries = true)
    public R<String> deleteByIds(@RequestParam List<Long> ids){
        //先删除套餐中存在setmealDish中的菜品信息和套餐的图片文件
        ids.stream().map((item) -> {
            //删除套餐中的菜品信息
            LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SetmealDish::getSetmealId,item);
            setmealDishService.remove(queryWrapper);

            //删除套餐的图片文件信息
            Setmeal setmeal = setmealService.getById(item);
            try {
                deletePicture.deleteByName(basePath+setmeal.getImage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //删除套餐信息
            setmealService.removeById(item);

            return item;
        }).collect(Collectors.toList());

        return R.success("删除成功！");
    }

    /**
     * 修改套餐状态为0，停售
     * @param ids
     * @return
     */
    @PostMapping("/status/0")
    @CacheEvict(value = "setmealCache" ,allEntries = true)
    public R<String> updateStatusTo0ByIds(@RequestParam List<Long> ids){
        //修改ids中id对应的套餐的状态status为0
        ids.stream().map((item) -> {
            //创建条件构造器对象
            LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(Setmeal::getStatus,0);
            updateWrapper.eq(Setmeal::getId,item);
            setmealService.update(updateWrapper);
            return item;
        }).collect(Collectors.toList());
        return R.success("修改成功！");
    }

    /**
     * 修改套餐状态为1，启售
     * @param ids
     * @return
     */
    @PostMapping("/status/1")
    @CacheEvict(value = "setmealCache" ,allEntries = true)
    public R<String> updateStatusTo1ByIds(@RequestParam List<Long> ids){
        //修改ids中id对应的套餐的状态status为1
        ids.stream().map((item) -> {
            //创建条件构造器对象
            LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(Setmeal::getStatus,1);
            updateWrapper.eq(Setmeal::getId,item);
            setmealService.update(updateWrapper);
            return item;
        }).collect(Collectors.toList());
        return R.success("修改成功！");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId+'_'+#setmeal.status")
    public R<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 根据套餐id查找套餐内菜品的信息
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    public R<List<Dish>> dish(@PathVariable Long id){
        //根据setmealId查找setmealDish信息
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,id);
        queryWrapper.orderByDesc(SetmealDish::getUpdateTime);
        List<SetmealDish> setmealDishList = setmealDishService.list(queryWrapper);
        //再根据setmealDishList查找出对应的dish的具体信息
        List<Dish> dishList = setmealDishList.stream().map((item) -> {
            //查找dish信息
            Dish dish = dishService.getById(item.getDishId());
            return dish;
        }).collect(Collectors.toList());
        return R.success(dishList);
    }


}
