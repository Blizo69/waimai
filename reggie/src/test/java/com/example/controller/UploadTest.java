package com.example.controller;

import java.util.Arrays;
import java.util.Comparator;

public class UploadTest {
    public int eraseOverlapIntervals(int[][] intervals) {
//将他们从小到大排序
        Arrays.sort(intervals ,new Comparator<int []>(){
            @Override
            public int compare(int[] a,int[] b){
                return a[0]-b[0];
            }
        });
        int len = intervals.length;
        int count = 0;
        int end = intervals[0][1];
        for(int i= 1;i<len ;i++){
            if(intervals[i][0] > end){
                end = intervals[i][1];
            }else{
                count++;
            }
        }
        return count;

    }
    public static void main(String[] args) {

    }
}
