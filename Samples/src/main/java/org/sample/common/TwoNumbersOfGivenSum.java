package org.sample.common;

import java.util.*;

public class TwoNumbersOfGivenSum {
    public static void main(String[] args) {
        TwoNumbersOfGivenSum instance = new TwoNumbersOfGivenSum();
        int[] nums = {2,24,11,15,7};
        int target = 9;
        int[] result = instance.twoSum(nums, target);
        System.out.println(result[0] +" and "+ result[1]);
    }

    public int[] twoSum(int[] nums, int target) {
        Map seen = new HashMap<Integer, Integer>();
        for(int i=0; i<nums.length; i++){
            int diff = target - nums[i];
            if(seen.get(diff) != null){
                return new  int[] {Integer.parseInt(seen.get(diff).toString()), i};
            }else{
                seen.put(nums[i], i);
            }
        }
        return new int[] {};
    }
}
