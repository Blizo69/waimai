package com.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
public class deletePicture {

    public static void deleteByName(String url) throws IOException {
        //读取文件
        File file = new File(url);
        boolean flag = file.delete();
        log.info("删除图片{}结果为{}",url,flag);
    }

}
