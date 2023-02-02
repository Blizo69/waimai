package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.common.BaseContext;
import com.example.common.R;
import com.example.entity.ShoppingCart;
import com.example.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
@Slf4j
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 加入购物车操作
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){

        //为该订单赋值用户id
        shoppingCart.setUserId(BaseContext.getCurrentId());

        //判断当前购物车表中是否存在相同的订单,当用户id，菜品/套餐id相同时判定为相同套餐
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(shoppingCart.getSetmealId() != null,ShoppingCart::getSetmealId,
                shoppingCart.getSetmealId());
        shoppingCartLambdaQueryWrapper.eq(shoppingCart.getDishId() != null,ShoppingCart::getDishId,
                shoppingCart.getDishId());
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId,shoppingCart.getUserId());
        ShoppingCart cart = shoppingCartService.getOne(shoppingCartLambdaQueryWrapper);

        if (cart == null){
            //不存在相同订单，直接添加进shoppingCart表中
            shoppingCart.setNumber(1);
            shoppingCartService.save(shoppingCart);
            cart = shoppingCart;

        }else{
            //存在相同的订单，修改订单中的number数值，在原基础上加1
//            log.info("原本的"+cart.getNumber().toString());
            LambdaUpdateWrapper<ShoppingCart> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ShoppingCart::getId,cart.getId());
            updateWrapper.set(ShoppingCart::getNumber,cart.getNumber()+1);
            shoppingCartService.update(updateWrapper);
//            log.info("修改后"+cart.getNumber().toString());
            cart.setNumber(cart.getNumber()+1);
        }
        return R.success(cart);
    }

    /**
     * 购物车中菜品/套餐订单的数量-1
     * @param shoppingCart
     * @return
     */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart){

        //利用dishId查出shoppingCart表中的订单
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getDishId,shoppingCart.getDishId());
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        ShoppingCart cartOne = shoppingCartService.getOne(queryWrapper);
        //判断该订单的数量是否是1
        if(cartOne.getNumber() == 1){
            //如果是1则删除该订单
            shoppingCart = cartOne;
            shoppingCart.setNumber(0);
            shoppingCartService.removeById(cartOne.getId());
            return R.success(shoppingCart);

        }else{
            //如果不是1，则在原来的基础上修改，使数量-1
            LambdaUpdateWrapper<ShoppingCart> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ShoppingCart::getId,cartOne.getId());
            updateWrapper.set(ShoppingCart::getNumber,cartOne.getNumber()-1);
            shoppingCartService.update(updateWrapper);
            cartOne.setNumber(cartOne.getNumber()-1);
            return R.success(cartOne);
        }
    }

    /**
     * 查找用户购物车信息
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){

        //获取当前用户的id
        Long currentId = BaseContext.getCurrentId();

        //利用当前用户id查找该用户的所有购物车订单数据
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,currentId);
        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

}
